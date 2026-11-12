package com.deadzone.messenger

import android.content.Context
import android.util.Log
import com.deadzone.messenger.crypto.CryptoManager
import com.deadzone.messenger.data.AvatarStore
import com.deadzone.messenger.data.ChatDatabase
import com.deadzone.messenger.data.ContactEntity
import com.deadzone.messenger.data.toEntity
import com.deadzone.messenger.data.toMeshMessage
import com.deadzone.messenger.model.MeshEndpoint
import com.deadzone.messenger.model.MeshMessage
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import android.util.Base64
import javax.crypto.spec.SecretKeySpec

data class IncomingMediaHeader(
    val id: String,
    val senderName: String,
    val senderDeviceId: String,
    val messageType: String,
    val timestamp: Long,
    val isBroadcast: Boolean,
    val targetDeviceId: String,
    val aesKeyBase64: String
)

class MeshNetworkManager(private val context: Context) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "com.deadzone.messenger.MESH_NETWORK"
    private val myDeviceId: String
    private val trustedDevices = mutableSetOf<String>()
    private val lastActivityTimestamps = ConcurrentHashMap<String, Long>()
    private val processedMessageIds = ConcurrentHashMap.newKeySet<String>()
    private val incomingMediaHeaders = ConcurrentHashMap<Long, IncomingMediaHeader>()
    private val incomingFilePayloads = ConcurrentHashMap<Long, Payload>()

    private var myName: String = "Unknown"

    private val _endpoints = MutableStateFlow<List<MeshEndpoint>>(emptyList())
    val endpoints: StateFlow<List<MeshEndpoint>> = _endpoints.asStateFlow()

    private val _messages = MutableSharedFlow<MeshMessage>(replay = 50)
    val messages: SharedFlow<MeshMessage> = _messages.asSharedFlow()

    private val db = ChatDatabase.getDatabase(context)
    private val messageDao = db.messageDao()
    private val contactDao = db.contactDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Message type constants ────────────────────────────────────────────────
    private companion object {
        const val TYPE_CHAT = "CHAT"
        const val TYPE_PROFILE_SYNC = "PROFILE_SYNC"
        const val TYPE_DELETE = "DELETE"
        const val TYPE_SAVE_SYNC = "SAVE_SYNC"
        const val TYPE_MEDIA_HEADER = "MEDIA_HEADER"
    }

    init {
        // Initialize persistent Device ID to prevent ghost self-discovery after rotation
        val prefs = context.getSharedPreferences("DeadzonePrefs", Context.MODE_PRIVATE)
        var savedId = prefs.getString("DEVICE_ID", null)
        if (savedId == null) {
            savedId = UUID.randomUUID().toString()
            prefs.edit().putString("DEVICE_ID", savedId).apply()
        }
        myDeviceId = savedId

        CryptoManager.initialize(context)

        trustedDevices.addAll(prefs.getStringSet("TRUSTED_DEVICES", emptySet()) ?: emptySet())

        // Auto-delete messages older than 24 hours (runs every hour while app is open)
        scope.launch {
            while (isActive) {
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                messageDao.deleteMessagesOlderThan(oneDayAgo)
                delay(60 * 60 * 1000)
            }
        }

        // Smart Idle Disconnect: Disconnect endpoints idle for > 3 minutes to save battery
        scope.launch {
            while (isActive) {
                delay(60 * 1000)
                val now = System.currentTimeMillis()
                val idleThreshold = 3 * 60 * 1000 // 3 minutes
                val iter = lastActivityTimestamps.iterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    if (now - entry.value > idleThreshold) {
                        Log.i("MeshNetwork", "Disconnecting idle endpoint: ${entry.key}")
                        connectionsClient.disconnectFromEndpoint(entry.key)
                        updateEndpoint(entry.key) { it.copy(isConnected = false) }
                        iter.remove()
                    }
                }
            }
        }
    }

    fun getChatMessages(chatGroupId: String) = messageDao.getMessagesForChat(chatGroupId).map { list ->
        list.map { it.toMeshMessage() }.reversed()
    }

    fun getContacts() = contactDao.getAllContacts()

    fun getUnreadCounts(): Flow<Map<String, Int>> = messageDao.getUnreadCounts()
        .map { list -> list.associate { it.chatGroupId to it.count } }

    fun markAsRead(chatGroupId: String) {
        scope.launch {
            messageDao.markMessagesAsRead(chatGroupId)
        }
    }

    // ── Profile sync ──────────────────────────────────────────────────────────

    /**
     * Send our name + avatar to a single endpoint (called right after connection is established).
     */
    private fun sendProfileSync(endpointId: String) {
        val myAvatar = AvatarStore.getMyAvatar(context)
        val myPublicKey = CryptoManager.getMyPublicKey(context)
        val json = JSONObject().apply {
            put("type", TYPE_PROFILE_SYNC)
            put("senderName", myName)
            put("deviceId", myDeviceId)
            if (myAvatar != null) put("avatarBase64", myAvatar)
            if (myPublicKey != null) put("publicKey", myPublicKey)
        }
        val payload = Payload.fromBytes(json.toString().toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
            .addOnFailureListener { Log.w("MeshNetwork", "ProfileSync send failed to $endpointId", it) }
    }

    /**
     * Broadcast our profile to ALL currently connected endpoints.
     * Called when the user changes their avatar.
     */
    fun broadcastProfileSync() {
        val connected = _endpoints.value.filter { it.isConnected }.map { it.id }
        if (connected.isEmpty()) return
        val myAvatar = AvatarStore.getMyAvatar(context)
        val myPublicKey = CryptoManager.getMyPublicKey(context)
        val json = JSONObject().apply {
            put("type", TYPE_PROFILE_SYNC)
            put("senderName", myName)
            put("deviceId", myDeviceId)
            if (myAvatar != null) put("avatarBase64", myAvatar)
            if (myPublicKey != null) put("publicKey", myPublicKey)
        }
        val payload = Payload.fromBytes(json.toString().toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(connected, payload)
    }

    // ── Payload callback ──────────────────────────────────────────────────────

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val jsonString = String(bytes, StandardCharsets.UTF_8)
                    try {
                        val json = JSONObject(jsonString)
                        val type = json.optString("type", TYPE_CHAT)

                        when (type) {
                            TYPE_PROFILE_SYNC -> handleProfileSync(endpointId, json)
                            TYPE_DELETE       -> handleDeleteMessage(endpointId, json, payload)
                            TYPE_SAVE_SYNC    -> handleSaveSync(endpointId, json, payload)
                            TYPE_MEDIA_HEADER -> handleMediaHeader(endpointId, json, payload)
                            else              -> handleChatMessage(endpointId, json, payload)
                        }
                    } catch (e: Exception) {
                        Log.e("MeshNetwork", "Failed to parse payload", e)
                    }
                }
            } else if (payload.type == Payload.Type.FILE) {
                incomingFilePayloads[payload.id] = payload
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                val header = incomingMediaHeaders.remove(update.payloadId)
                val filePayload = incomingFilePayloads.remove(update.payloadId)
                if (header != null && filePayload != null) {
                    processIncomingMediaFile(endpointId, update.payloadId, header, filePayload)
                }
            }
        }
    }

    private fun handleProfileSync(endpointId: String, json: JSONObject) {
        val senderDeviceId = json.optString("deviceId", "")
        val senderName = json.optString("senderName", "Unknown")
        val avatarBase64 = json.optString("avatarBase64", "").takeIf { it.isNotBlank() }
        val publicKey = json.optString("publicKey", "").takeIf { it.isNotBlank() }

        Log.i("MeshNetwork", "ProfileSync received from $senderName ($senderDeviceId)")

        // Update the in-memory endpoint avatar
        updateEndpoint(endpointId) { it.copy(avatarBase64 = avatarBase64) }

        // Persist avatar in SharedPreferences (fast) and Room (for contacts list)
        if (senderDeviceId.isNotBlank()) {
            if (avatarBase64 != null) {
                AvatarStore.savePeerAvatar(context, senderDeviceId, avatarBase64)
            }
            scope.launch {
                val existing = contactDao.getContact(senderDeviceId)
                if (existing != null) {
                    contactDao.insertOrUpdateContact(
                        existing.copy(
                            displayName = senderName,
                            lastSeenTimestamp = System.currentTimeMillis(),
                            avatarBase64 = avatarBase64 ?: existing.avatarBase64,
                            publicKey = publicKey ?: existing.publicKey
                        )
                    )
                } else {
                    // First time seeing this peer – create a contact record
                    contactDao.insertOrUpdateContact(
                        ContactEntity(
                            deviceId = senderDeviceId,
                            displayName = senderName,
                            lastSeenTimestamp = System.currentTimeMillis(),
                            avatarBase64 = avatarBase64,
                            publicKey = publicKey
                        )
                    )
                }
            }
        }
    }

    private fun handleDeleteMessage(endpointId: String, json: JSONObject, originalPayload: Payload) {
        val id = json.optString("id", "")
        if (id.isEmpty()) return

        // Prevent routing loops
        if (!processedMessageIds.add(id + "_delete")) return
        if (processedMessageIds.size > 10000) processedMessageIds.clear()

        Log.i("MeshNetwork", "Delete request received for message: $id")

        scope.launch {
            messageDao.deleteMessageById(id)
        }

        // Mesh Routing for Deletions: Forward to all other connected nodes
        val isBroadcast = json.optBoolean("isBroadcast", true)
        val targetDeviceId = json.optString("targetDeviceId", "")
        val isForMe = targetDeviceId.isEmpty() || targetDeviceId == myDeviceId

        if (isBroadcast || !isForMe) {
            val connectedEndpoints = _endpoints.value.filter { it.isConnected && it.id != endpointId }.map { it.id }
            if (connectedEndpoints.isNotEmpty()) {
                connectionsClient.sendPayload(connectedEndpoints, originalPayload)
            }
        }
    }

    private fun handleSaveSync(endpointId: String, json: JSONObject, originalPayload: Payload) {
        val id = json.optString("id", "")
        val isSaved = json.optBoolean("isSaved", false)
        if (id.isEmpty()) return

        // Prevent routing loops
        val loopId = "${id}_save_${isSaved}"
        if (!processedMessageIds.add(loopId)) return
        if (processedMessageIds.size > 10000) processedMessageIds.clear()

        Log.i("MeshNetwork", "Save sync received for message: $id (saved=$isSaved)")

        scope.launch {
            messageDao.updateSavedStatus(id, isSaved)
        }

        // Mesh Routing: Forward to all other connected nodes
        val isBroadcast = json.optBoolean("isBroadcast", true)
        val targetDeviceId = json.optString("targetDeviceId", "")
        val isForMe = targetDeviceId.isEmpty() || targetDeviceId == myDeviceId

        if (isBroadcast || !isForMe) {
            val connectedEndpoints = _endpoints.value.filter { it.isConnected && it.id != endpointId }.map { it.id }
            if (connectedEndpoints.isNotEmpty()) {
                connectionsClient.sendPayload(connectedEndpoints, originalPayload)
            }
        }
    }

    private fun handleMediaHeader(endpointId: String, json: JSONObject, originalPayload: Payload) {
        val payloadId = json.getLong("payloadId")
        val header = IncomingMediaHeader(
            id = json.getString("id"),
            senderName = json.getString("senderName"),
            senderDeviceId = json.getString("senderDeviceId"),
            messageType = json.getString("messageType"),
            timestamp = json.getLong("timestamp"),
            isBroadcast = json.optBoolean("isBroadcast", false),
            targetDeviceId = json.optString("targetDeviceId", ""),
            aesKeyBase64 = json.getString("aesKeyBase64")
        )
        incomingMediaHeaders[payloadId] = header

        // Forward header if needed
        val isForMe = header.targetDeviceId.isEmpty() || header.targetDeviceId == myDeviceId
        if (header.isBroadcast || !isForMe) {
            val connectedEndpoints = _endpoints.value.filter { it.isConnected && it.id != endpointId }.map { it.id }
            if (connectedEndpoints.isNotEmpty()) {
                connectionsClient.sendPayload(connectedEndpoints, originalPayload)
            }
        }
    }

    private fun processIncomingMediaFile(endpointId: String, payloadId: Long, header: IncomingMediaHeader, filePayload: Payload) {
        val id = header.id
        if (!processedMessageIds.add(id)) return
        if (processedMessageIds.size > 10000) {
            processedMessageIds.clear()
            processedMessageIds.add(id)
        }

        val isForMe = header.targetDeviceId.isEmpty() || header.targetDeviceId == myDeviceId
        val parcelFileDescriptor = filePayload.asFile()?.asParcelFileDescriptor()
        val encryptedFile = filePayload.asFile()?.asJavaFile()

        if (parcelFileDescriptor == null && encryptedFile == null) return

        // Forward file payload if needed
        if (header.isBroadcast || !isForMe) {
            val connectedEndpoints = _endpoints.value.filter { it.isConnected && it.id != endpointId }.map { it.id }
            if (connectedEndpoints.isNotEmpty()) {
                connectionsClient.sendPayload(connectedEndpoints, filePayload)
            }
        }

        if (isForMe) {
            scope.launch {
                val aesKey = if (!header.isBroadcast) {
                    CryptoManager.decryptAesKeyFromPeer(context, header.aesKeyBase64)
                } else {
                    SecretKeySpec(Base64.decode(header.aesKeyBase64, Base64.DEFAULT), "AES")
                }

                val decryptedFile = java.io.File(context.filesDir, "media_${id}")
                if (aesKey != null) {
                    try {
                        val inputStream = parcelFileDescriptor?.let {
                            android.os.ParcelFileDescriptor.AutoCloseInputStream(it)
                        } ?: java.io.FileInputStream(encryptedFile!!)
                        
                        CryptoManager.decryptFile(inputStream, decryptedFile, aesKey)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val chatGroupId = if (header.isBroadcast) "GROUP" else header.senderDeviceId

                val msg = MeshMessage(
                    id = id,
                    senderName = header.senderName,
                    content = "[Media: ${header.messageType}]",
                    timestamp = header.timestamp,
                    isFromMe = false,
                    chatGroupId = chatGroupId,
                    isSaved = false,
                    isRead = false,
                    messageType = header.messageType,
                    localFilePath = if (decryptedFile.exists()) decryptedFile.absolutePath else null
                )

                _messages.tryEmit(msg)
                lastActivityTimestamps[endpointId] = System.currentTimeMillis()
                messageDao.insertMessage(msg.toEntity(chatGroupId))
                // Cleanup temp encrypted file
                encryptedFile?.delete()
            }
        } else {
            // Delete temp file if it's not for us and we just forwarded it
            encryptedFile?.delete()
        }
    }

    private fun handleChatMessage(endpointId: String, json: JSONObject, originalPayload: Payload) {
        val id = json.getString("id")

        // Prevent routing loops — clear when too large, then re-add the current id
        if (!processedMessageIds.add(id)) return
        if (processedMessageIds.size > 10000) {
            processedMessageIds.clear()
            processedMessageIds.add(id) // Re-add so it's not re-processed immediately
        }

        val isBroadcast = json.optBoolean("isBroadcast", false)
        val targetDeviceId = json.optString("targetDeviceId", "")
        val isForMe = targetDeviceId.isEmpty() || targetDeviceId == myDeviceId
        
        val rawContent = json.getString("content")
        val finalContent = if (isForMe && !isBroadcast) {
            CryptoManager.decrypt(context, rawContent)
        } else {
            rawContent
        }

        val msg = MeshMessage(
            id = id,
            senderName = json.getString("senderName"),
            content = finalContent,
            timestamp = json.getLong("timestamp"),
            isFromMe = false,
            isSaved = json.optBoolean("isSaved", false),
            isRead = false
        )

        if (isForMe) {
            val senderDeviceId = json.optString("senderDeviceId").takeIf { it.isNotBlank() } 
                ?: _endpoints.value.find { it.id == endpointId }?.deviceId 
                ?: msg.senderName
                
            val chatGroupId = if (isBroadcast) "GROUP" else senderDeviceId

            val enhancedMsg = msg.copy(chatGroupId = chatGroupId)
            _messages.tryEmit(enhancedMsg)

            lastActivityTimestamps[endpointId] = System.currentTimeMillis()

            scope.launch {
                messageDao.insertMessage(enhancedMsg.toEntity(chatGroupId))
            }
        }

        // Mesh Routing: Forward to all other connected nodes
        if (isBroadcast || !isForMe) {
            val connectedEndpoints = _endpoints.value.filter { it.isConnected && it.id != endpointId }.map { it.id }
            if (connectedEndpoints.isNotEmpty()) {
                connectionsClient.sendPayload(connectedEndpoints, originalPayload)
            }
        }
    }

    // ── Connection lifecycle ──────────────────────────────────────────────────

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val parts = connectionInfo.endpointName.split("|", limit = 2)
            val parsedName = parts.firstOrNull() ?: connectionInfo.endpointName
            val deviceId = parts.getOrNull(1)

            Log.i("MeshNetwork", "Connection Initiated with $parsedName")

            val isTrusted = deviceId != null && trustedDevices.contains(deviceId)

            // Load cached avatar for this peer (may be null if first time)
            val cachedAvatar = deviceId?.let { AvatarStore.getPeerAvatar(context, it) }

            if (isTrusted) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                    .addOnFailureListener {
                        Log.e("MeshNetwork", "Failed to auto-accept $endpointId", it)
                    }

                scope.launch {
                    val existing = contactDao.getContact(deviceId!!)
                    val avatarToSave = cachedAvatar ?: existing?.avatarBase64
                    contactDao.insertOrUpdateContact(ContactEntity(deviceId, parsedName, System.currentTimeMillis(), avatarToSave))
                }

                if (_endpoints.value.none { it.id == endpointId }) {
                    _endpoints.update { list ->
                        list + MeshEndpoint(
                            id = endpointId,
                            name = parsedName,
                            deviceId = deviceId,
                            connectionRequested = false,
                            avatarBase64 = cachedAvatar
                        )
                    }
                }
            } else {
                if (_endpoints.value.none { it.id == endpointId }) {
                    _endpoints.update { list ->
                        list + MeshEndpoint(
                            id = endpointId,
                            name = parsedName,
                            deviceId = deviceId,
                            connectionRequested = true,
                            authenticationToken = connectionInfo.authenticationDigits,
                            isIncomingRequest = connectionInfo.isIncomingConnection,
                            avatarBase64 = cachedAvatar
                        )
                    }
                } else {
                    updateEndpoint(endpointId) {
                        it.copy(
                            connectionRequested = true,
                            authenticationToken = connectionInfo.authenticationDigits,
                            isIncomingRequest = connectionInfo.isIncomingConnection,
                            deviceId = deviceId,
                            avatarBase64 = cachedAvatar
                        )
                    }
                }
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.i("MeshNetwork", "Connected to $endpointId")
                lastActivityTimestamps[endpointId] = System.currentTimeMillis()
                updateEndpoint(endpointId) {
                    it.copy(isConnected = true, connectionRequested = false, authenticationToken = null)
                }

                // Send our profile immediately after connection is established
                sendProfileSync(endpointId)

                // Sync unsent offline messages
                val endpoint = _endpoints.value.find { it.id == endpointId }
                endpoint?.deviceId?.let { deviceId ->
                    scope.launch {
                        val unsent = messageDao.getUnsentMessages(deviceId)
                        for (entity in unsent) {
                            val msg = entity.toMeshMessage()
                            val json = JSONObject().apply {
                                put("type", TYPE_CHAT)
                                put("id", msg.id)
                                put("senderName", msg.senderName)
                                put("senderDeviceId", myDeviceId)
                                put("content", msg.content)
                                put("timestamp", msg.timestamp)
                                put("isBroadcast", false)
                                put("targetDeviceId", deviceId)
                            }
                            val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
                            val payload = Payload.fromBytes(bytes)
                            connectionsClient.sendPayload(endpointId, payload)
                            messageDao.markMessageAsSent(msg.id)
                            delay(100)
                        }
                    }
                }
            } else {
                Log.e("MeshNetwork", "Connection failed with $endpointId: ${result.status.statusCode}")
                updateEndpoint(endpointId) {
                    it.copy(isConnected = false, connectionRequested = false, authenticationToken = null)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i("MeshNetwork", "Disconnected from $endpointId")
            lastActivityTimestamps.remove(endpointId)
            updateEndpoint(endpointId) {
                it.copy(isConnected = false, connectionRequested = false, authenticationToken = null)
            }
        }
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val parts = info.endpointName.split("|", limit = 2)
            val parsedName = parts.firstOrNull() ?: info.endpointName
            val discoveredDeviceId = parts.getOrNull(1)

            if (discoveredDeviceId == myDeviceId) return // Ignore our own device

            Log.i("MeshNetwork", "Endpoint found: $parsedName")

            // Pre-load cached avatar so card shows something immediately
            val cachedAvatar = discoveredDeviceId?.let { AvatarStore.getPeerAvatar(context, it) }

            if (_endpoints.value.none { it.id == endpointId }) {
                _endpoints.update { list ->
                    list + MeshEndpoint(
                        id = endpointId,
                        name = parsedName,
                        deviceId = discoveredDeviceId,
                        avatarBase64 = cachedAvatar
                    )
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i("MeshNetwork", "Endpoint lost: $endpointId")
            _endpoints.update { list -> list.filter { it.id != endpointId } }
        }
    }

    // ── Mesh control ──────────────────────────────────────────────────────────

    fun startMesh(displayName: String) {
        myName = displayName
        val advertisedName = "$myName|$myDeviceId"

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startAdvertising(
            advertisedName, serviceId, connectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            Log.i("MeshNetwork", "Advertising started")
        }.addOnFailureListener {
            Log.e("MeshNetwork", "Advertising failed", it)
        }

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startDiscovery(
            serviceId, endpointDiscoveryCallback, discoveryOptions
        ).addOnSuccessListener {
            Log.i("MeshNetwork", "Discovery started")
        }.addOnFailureListener {
            Log.e("MeshNetwork", "Discovery failed", it)
        }
    }

    fun stopMesh() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _endpoints.value = emptyList()
    }

    fun destroy() {
        scope.cancel() // Cancel all background coroutines
    }

    fun requestConnection(endpointId: String) {
        val advertisedName = "$myName|$myDeviceId"
        connectionsClient.requestConnection(advertisedName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener {
                Log.e("MeshNetwork", "Failed to request connection to $endpointId", it)
            }
    }

    fun acceptConnection(endpointId: String) {
        val endpoint = _endpoints.value.find { it.id == endpointId }
        endpoint?.deviceId?.let { deviceId ->
            trustedDevices.add(deviceId)
            val prefs = context.getSharedPreferences("DeadzonePrefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("TRUSTED_DEVICES", trustedDevices.toSet()).apply()

            scope.launch {
                val existing = contactDao.getContact(deviceId)
                val avatarToSave = AvatarStore.getPeerAvatar(context, deviceId) ?: existing?.avatarBase64
                contactDao.insertOrUpdateContact(ContactEntity(deviceId, endpoint.name, System.currentTimeMillis(), avatarToSave))
            }
        }

        connectionsClient.acceptConnection(endpointId, payloadCallback)
            .addOnFailureListener {
                Log.e("MeshNetwork", "Failed to accept connection from $endpointId", it)
            }
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
        updateEndpoint(endpointId) {
            it.copy(connectionRequested = false, authenticationToken = null)
        }
    }

    fun sendMessage(content: String, targetDeviceId: String? = null) {
        val chatGroupId = targetDeviceId ?: "GROUP"
        val msg = MeshMessage(
            id = UUID.randomUUID().toString(),
            senderName = myName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            chatGroupId = chatGroupId
        )

        processedMessageIds.add(msg.id)
        _messages.tryEmit(msg) // Show in UI immediately
        val isBroadcast = targetDeviceId == null

        scope.launch {
            var finalContent = msg.content
            if (targetDeviceId != null) {
                val existing = contactDao.getContact(targetDeviceId)
                if (existing?.publicKey != null) {
                    finalContent = CryptoManager.encrypt(msg.content, existing.publicKey)
                }
            }

            val json = JSONObject().apply {
                put("type", TYPE_CHAT)
                put("id", msg.id)
                put("senderName", msg.senderName)
                put("senderDeviceId", myDeviceId)
                put("content", finalContent)
                put("timestamp", msg.timestamp)
                put("isBroadcast", isBroadcast)
                put("isSaved", false)
                if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
            }
            val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
            val payload = Payload.fromBytes(bytes)

            if (targetDeviceId != null) {
                val targetEndpoint = _endpoints.value.find { it.deviceId == targetDeviceId }
                val connectedEndpoints = _endpoints.value.filter { it.isConnected }.map { it.id }
                val canRoute = connectedEndpoints.isNotEmpty()

                val existing = contactDao.getContact(targetDeviceId)
                val targetName = targetEndpoint?.name ?: existing?.displayName ?: "Unknown"
                val avatarToSave = targetEndpoint?.avatarBase64 ?: existing?.avatarBase64
                
                contactDao.insertOrUpdateContact(ContactEntity(
                    deviceId = targetDeviceId,
                    displayName = targetName,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    avatarBase64 = avatarToSave,
                    publicKey = existing?.publicKey
                ))
                
                messageDao.insertMessage(msg.toEntity(chatGroupId).copy(isSent = canRoute))

                if (canRoute) {
                    connectionsClient.sendPayload(connectedEndpoints, payload)
                    connectedEndpoints.forEach { lastActivityTimestamps[it] = System.currentTimeMillis() }
                } else {
                    Log.w("MeshNetwork", "No connected endpoints to route message to: $targetDeviceId. Queued.")
                    if (targetEndpoint != null) requestConnection(targetEndpoint.id)
                }
            } else {
                val connectedEndpoints = _endpoints.value.filter { it.isConnected }.map { it.id }
                if (connectedEndpoints.isNotEmpty()) {
                    connectionsClient.sendPayload(connectedEndpoints, payload)
                    connectedEndpoints.forEach { lastActivityTimestamps[it] = System.currentTimeMillis() }
                }
                messageDao.insertMessage(msg.toEntity(chatGroupId))
            }
        }
    }

    fun sendMediaMessage(messageType: String, file: java.io.File, targetDeviceId: String? = null) {
        val chatGroupId = targetDeviceId ?: "GROUP"
        val msg = MeshMessage(
            id = UUID.randomUUID().toString(),
            senderName = myName,
            content = "[Media: $messageType]",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            chatGroupId = chatGroupId,
            messageType = messageType,
            localFilePath = file.absolutePath
        )

        processedMessageIds.add(msg.id)
        _messages.tryEmit(msg) 
        val isBroadcast = targetDeviceId == null

        scope.launch {
            try {
                val aesKey = CryptoManager.generateAesKey()
                val encryptedFile = java.io.File(context.cacheDir, "enc_${msg.id}")
                CryptoManager.encryptFile(file, encryptedFile, aesKey)

                val filePayload = Payload.fromFile(encryptedFile)
                val filePayloadId = filePayload.id

                var encryptedAesKeyStr = Base64.encodeToString(aesKey.encoded, Base64.NO_WRAP)

                if (targetDeviceId != null) {
                    val existing = contactDao.getContact(targetDeviceId)
                    if (existing?.publicKey != null) {
                        val encryptedKey = CryptoManager.encryptAesKeyForPeer(aesKey, existing.publicKey)
                        if (encryptedKey != null) encryptedAesKeyStr = encryptedKey
                    }
                }

                val json = JSONObject().apply {
                    put("type", TYPE_MEDIA_HEADER)
                    put("id", msg.id)
                    put("payloadId", filePayloadId)
                    put("senderName", msg.senderName)
                    put("senderDeviceId", myDeviceId)
                    put("messageType", messageType)
                    put("timestamp", msg.timestamp)
                    put("isBroadcast", isBroadcast)
                    put("isSaved", false)
                    put("aesKeyBase64", encryptedAesKeyStr)
                    if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
                }
                
                val headerBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
                val headerPayload = Payload.fromBytes(headerBytes)

                val chatGroupTarget = chatGroupId

                if (targetDeviceId != null) {
                    val targetEndpoint = _endpoints.value.find { it.deviceId == targetDeviceId }
                    val connectedEndpoints = _endpoints.value.filter { it.isConnected }.map { it.id }
                    val canRoute = connectedEndpoints.isNotEmpty()

                    messageDao.insertMessage(msg.toEntity(chatGroupTarget).copy(isSent = canRoute))

                    if (canRoute) {
                        connectionsClient.sendPayload(connectedEndpoints, headerPayload)
                        connectionsClient.sendPayload(connectedEndpoints, filePayload)
                        connectedEndpoints.forEach { lastActivityTimestamps[it] = System.currentTimeMillis() }
                    } else {
                        Log.w("MeshNetwork", "No connected endpoints. Queued.")
                        if (targetEndpoint != null) requestConnection(targetEndpoint.id)
                    }
                } else {
                    val connectedEndpoints = _endpoints.value.filter { it.isConnected }.map { it.id }
                    if (connectedEndpoints.isNotEmpty()) {
                        connectionsClient.sendPayload(connectedEndpoints, headerPayload)
                        connectionsClient.sendPayload(connectedEndpoints, filePayload)
                        connectedEndpoints.forEach { lastActivityTimestamps[it] = System.currentTimeMillis() }
                    }
                    messageDao.insertMessage(msg.toEntity(chatGroupTarget))
                }
            } catch (e: Exception) {
                Log.e("MeshNetwork", "Failed to send media", e)
            }
        }
    }

    fun deleteMessage(messageId: String, targetDeviceId: String? = null) {
        Log.i("MeshNetwork", "Deleting message: $messageId for everyone")

        scope.launch {
            messageDao.deleteMessageById(messageId)
        }

        val json = JSONObject().apply {
            put("type", TYPE_DELETE)
            put("id", messageId)
            put("isBroadcast", targetDeviceId == null)
            if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
        }
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val payload = Payload.fromBytes(bytes)

        val connectedEndpoints = _endpoints.value.filter { it.isConnected }.map { it.id }
        if (connectedEndpoints.isNotEmpty()) {
            connectionsClient.sendPayload(connectedEndpoints, payload)
        }
    }

    fun deleteMessageLocally(messageId: String) {
        Log.i("MeshNetwork", "Deleting message locally: $messageId")
        scope.launch {
            messageDao.deleteMessageById(messageId)
        }
    }

    fun toggleSaveMessage(messageId: String, isSaved: Boolean, targetDeviceId: String? = null) {
        Log.i("MeshNetwork", "Toggling save for message: $messageId to $isSaved")

        scope.launch {
            messageDao.updateSavedStatus(messageId, isSaved)
        }

        val json = JSONObject().apply {
            put("type", TYPE_SAVE_SYNC)
            put("id", messageId)
            put("isSaved", isSaved)
            put("isBroadcast", targetDeviceId == null)
            if (targetDeviceId != null) put("targetDeviceId", targetDeviceId)
        }
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val payload = Payload.fromBytes(bytes)

        val connectedEndpoints = _endpoints.value.filter { it.isConnected }.map { it.id }
        if (connectedEndpoints.isNotEmpty()) {
            connectionsClient.sendPayload(connectedEndpoints, payload)
        }
    }

    private fun updateEndpoint(endpointId: String, updateOp: (MeshEndpoint) -> MeshEndpoint) {
        _endpoints.update { list ->
            list.map { if (it.id == endpointId) updateOp(it) else it }
        }
    }
}
