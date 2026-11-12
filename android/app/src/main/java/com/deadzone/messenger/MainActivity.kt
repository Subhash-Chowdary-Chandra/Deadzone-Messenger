package com.deadzone.messenger

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deadzone.messenger.data.AvatarStore
import com.deadzone.messenger.data.ContactEntity
import com.deadzone.messenger.ui.ChatScreen
import com.deadzone.messenger.ui.RadarScreen
import com.deadzone.messenger.ui.AboutScreen
import com.deadzone.messenger.ui.theme.AccentNeon
import com.deadzone.messenger.ui.theme.BackgroundDark
import com.deadzone.messenger.ui.theme.DeadzoneMessengerTheme

class MainActivity : ComponentActivity() {

    private var meshService: MeshService? = null
    private var isBound = mutableStateOf(false)
    private var permissionsGrantedState = mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> 
            permissionsGrantedState.value = hasRequiredPermissions()
            if (permissionsGrantedState.value) {
                startAppServices()
            }
        }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MeshService.LocalBinder
            meshService = binder.getService()
            isBound.value = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        permissionsGrantedState.value = hasRequiredPermissions()
        if (permissionsGrantedState.value) {
            startAppServices()
        }

        val sharedPrefs = getSharedPreferences("DeadzonePrefs", Context.MODE_PRIVATE)
        val savedName = sharedPrefs.getString("DISPLAY_NAME", "") ?: ""
        val startRoute = intent?.getStringExtra("navigate_route") ?: "radar"

        setContent {
            DeadzoneMessengerTheme {
                if (!permissionsGrantedState.value) {
                    com.deadzone.messenger.ui.OnboardingScreen(
                        onGrantPermissionsClick = {
                            requestPermissionLauncher.launch(getRequiredPermissions().toTypedArray())
                        }
                    )
                    return@DeadzoneMessengerTheme
                }

                if (!isBound.value || meshService == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(BackgroundDark),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentNeon)
                    }
                    return@DeadzoneMessengerTheme
                }

                val meshManager = meshService!!.meshManager

                var displayName by remember { mutableStateOf(savedName) }
                var isNameSet by remember { mutableStateOf(savedName.isNotBlank()) }
                // Live avatar state – re-reads from prefs when updated
                var myAvatar by remember { mutableStateOf(AvatarStore.getMyAvatar(this@MainActivity)) }

                LaunchedEffect(isNameSet, displayName) {
                    if (isNameSet) {
                        meshManager.stopMesh()
                        meshManager.startMesh(displayName)
                    }
                }

                if (!isNameSet) {
                    NamePromptScreen(
                        onNameSubmit = { name ->
                            if (name.isNotBlank()) {
                                sharedPrefs.edit().putString("DISPLAY_NAME", name).apply()
                                displayName = name
                                isNameSet = true
                            }
                        }
                    )
                } else {
                    AppNavigation(
                        meshManager = meshManager,
                        myName = displayName,
                        myAvatar = myAvatar,
                        context = this@MainActivity,
                        onEditName = { newName ->
                            sharedPrefs.edit().putString("DISPLAY_NAME", newName).apply()
                            displayName = newName
                        },
                        onAvatarChanged = { base64 ->
                            myAvatar = base64
                            // Notify all connected peers of the new avatar
                            meshManager.broadcastProfileSync()
                        },
                        startRoute = startRoute
                    )
                }
            }
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        permissions.add(Manifest.permission.RECORD_AUDIO)

        return permissions
    }

    private fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startAppServices() {
        createNotificationChannel()
        Intent(this, MeshService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Mesh Messages"
            val descriptionText = "Notifications for incoming mesh messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("MESH_MESSAGES", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound.value) {
            unbindService(connection)
            isBound.value = false
        }
    }
}

@Composable
fun NamePromptScreen(onNameSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to Deadzone Messenger", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter Display Name") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onNameSubmit(text) }) {
                Text("Start")
            }
        }
    }
}

@Composable
fun AppNavigation(
    meshManager: MeshNetworkManager,
    myName: String,
    myAvatar: String?,
    context: MainActivity,
    onEditName: (String) -> Unit,
    onAvatarChanged: (String) -> Unit,
    startRoute: String = "radar"
) {
    val navController = rememberNavController()
    val endpoints by meshManager.endpoints.collectAsState()
    val messages by meshManager.messages.collectAsState(initial = null)
    val contacts by meshManager.getContacts().collectAsState(initial = emptyList())
    val unreadCounts by meshManager.getUnreadCounts().collectAsState(initial = emptyMap())

    val allMessages = remember { mutableStateListOf<com.deadzone.messenger.model.MeshMessage>() }
    LaunchedEffect(messages) {
        messages?.let {
            if (!allMessages.contains(it)) {
                allMessages.add(it)
            }
        }
    }

    var isOffline by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = startRoute) {
        composable("radar") {
            RadarScreen(
                endpoints = endpoints,
                contacts = contacts,
                unreadCounts = unreadCounts,
                myName = myName,
                myAvatar = myAvatar,
                isOffline = isOffline,
                onToggleOffline = { offline ->
                    isOffline = offline
                    if (offline) {
                        meshManager.stopMesh()
                    } else {
                        meshManager.startMesh(myName)
                    }
                },
                onEditName = onEditName,
                onAvatarChanged = onAvatarChanged,
                onRequestConnection = { meshManager.requestConnection(it) },
                onAcceptConnection = { meshManager.acceptConnection(it) },
                onRejectConnection = { meshManager.rejectConnection(it) },
                onContactClick = { deviceId -> navController.navigate("chat/device/$deviceId") },
                onAboutClick = { navController.navigate("about") }
            )
        }

        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable("chat/device/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            LaunchedEffect(deviceId) {
                meshManager.markAsRead(deviceId)
            }
            val contact = contacts.find { it.deviceId == deviceId }
            val endpoint = endpoints.find { it.deviceId == deviceId }
            val title = contact?.displayName ?: endpoint?.name ?: "Unknown Contact"

            // Use live endpoint avatar first (freshest), fall back to persisted contact avatar
            val peerAvatar = endpoint?.avatarBase64 ?: contact?.avatarBase64
                ?: AvatarStore.getPeerAvatar(context, deviceId)

            val dbMessages by meshManager.getChatMessages(deviceId).collectAsState(initial = emptyList())

            ChatScreen(
                title = title + if (endpoint?.isConnected == true) " (Online)" else " (Offline)",
                messages = dbMessages,
                avatarBase64 = peerAvatar,
                onSendMessage = { content -> meshManager.sendMessage(content, deviceId) },
                onSendMedia = { type, file -> meshManager.sendMediaMessage(type, file, deviceId) },
                onDeleteMessage = { id -> meshManager.deleteMessage(id, deviceId) },
                onDeleteMessageLocally = { id -> meshManager.deleteMessageLocally(id) },
                onToggleSave = { id, saved -> meshManager.toggleSaveMessage(id, saved, deviceId) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
