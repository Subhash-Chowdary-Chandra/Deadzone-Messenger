package com.deadzone.messenger.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deadzone.messenger.data.AvatarStore
import com.deadzone.messenger.data.ContactEntity
import com.deadzone.messenger.model.MeshEndpoint
import com.deadzone.messenger.ui.theme.*

// ── Reusable Avatar Composable ────────────────────────────────────────────────

/**
 * Circular avatar that shows [avatarBase64] as an image, or an initials badge
 * derived from [name] as a fallback.
 */
@Composable
fun AvatarImage(
    name: String,
    avatarBase64: String?,
    size: Dp = 40.dp,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
) {
    val bitmap: Bitmap? = remember(avatarBase64) {
        avatarBase64?.let { AvatarStore.base64ToBitmap(it) }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (borderWidth > 0.dp)
                    Modifier.border(borderWidth, borderColor, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Avatar of $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Initials fallback
            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val bgColor = remember(name) {
                val colors = listOf(
                    AccentNeon.copy(alpha = 0.25f),
                    AccentPurple.copy(alpha = 0.25f),
                    AccentBlue.copy(alpha = 0.25f)
                )
                colors[name.hashCode().and(0x7FFFFFFF) % colors.size]
            }
            val fgColor = remember(name) {
                val colors = listOf(AccentNeon, AccentPurple, AccentBlue)
                colors[name.hashCode().and(0x7FFFFFFF) % colors.size]
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = fgColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }
    }
}

// ── Radar Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    endpoints: List<MeshEndpoint>,
    contacts: List<ContactEntity>,
    unreadCounts: Map<String, Int>,
    myName: String,
    myAvatar: String?,
    isOffline: Boolean,
    onToggleOffline: (Boolean) -> Unit,
    onEditName: (String) -> Unit,
    onAvatarChanged: (String) -> Unit,   // base64 of the newly picked avatar
    onRequestConnection: (String) -> Unit,
    onAcceptConnection: (String) -> Unit,
    onRejectConnection: (String) -> Unit,
    onContactClick: (String) -> Unit,
    onAboutClick: () -> Unit
) {
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarMenu by remember { mutableStateOf(false) }
    var editingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Image picker – opens editor before saving
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val raw = BitmapFactory.decodeStream(stream)
            stream.close()
            if (raw != null) editingBitmap = raw
        } catch (e: Exception) {
            android.util.Log.e("RadarScreen", "Failed to load image", e)
        }
    }

    // Radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Restart),
        label = "radar_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Restart),
        label = "radar_alpha"
    )

    // ── Edit Name Dialog ──────────────────────────────────────────────────────
    if (showEditDialog) {
        var newName by remember { mutableStateOf(myName) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Display Name", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onEditName(newName)
                        showEditDialog = false
                    }
                }) { Text("Save", color = AccentNeon) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = CardDarkOpaque
        )
    }

    // ── Avatar Options DropdownMenu ───────────────────────────────────────────
    if (showAvatarMenu) {
        AlertDialog(
            onDismissRequest = { showAvatarMenu = false },
            title = { Text("Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Large preview
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AvatarImage(
                            name = myName,
                            avatarBase64 = myAvatar,
                            size = 88.dp,
                            borderColor = AccentNeon,
                            borderWidth = 2.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(myName, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAvatarMenu = false
                    imagePicker.launch("image/*")
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentNeon,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Photo", color = AccentNeon)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAvatarMenu = false
                    showEditDialog = true
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Name", color = TextSecondary)
                }
            },
            containerColor = CardDarkOpaque
        )
    }

    // ── Screen Layout ─────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tappable own avatar in top bar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { showAvatarMenu = true }
                    ) {
                        AvatarImage(
                            name = myName,
                            avatarBase64 = myAvatar,
                            size = 36.dp,
                            borderColor = AccentNeon,
                            borderWidth = 1.5.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Hi, $myName", color = TextPrimary, fontSize = 17.sp)
                }
            },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOffline) "Offline" else "Online", 
                        color = if (isOffline) TextSecondary else AccentNeon, 
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = !isOffline,
                        onCheckedChange = { onToggleOffline(!it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BackgroundDark,
                            checkedTrackColor = AccentNeon,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardDark
                        )
                    )
                    IconButton(onClick = onAboutClick) {
                        Icon(Icons.Default.Info, contentDescription = "About", tint = AccentNeon)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Radar Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                if (!isOffline) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(AccentNeon.copy(alpha = alpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(CardDarkOpaque)
                )
                
                // Nearby Devices Blips
                endpoints.forEach { endpoint ->
                    val hash = endpoint.id.hashCode()
                    val angle = (hash % 360) * (Math.PI / 180.0)
                    val radius = 40.0 + (kotlin.math.abs(hash) % 45)
                    
                    val x = (radius * kotlin.math.cos(angle)).toFloat()
                    val y = (radius * kotlin.math.sin(angle)).toFloat()
                    
                    val blipAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = kotlin.math.abs(hash) % 1000), 
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "blip_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = x.dp, y = y.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (endpoint.isConnected) AccentPurple.copy(alpha = blipAlpha)
                                else AccentNeon.copy(alpha = blipAlpha)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Nearby Devices (${endpoints.size})",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(endpoints) { endpoint ->
                    EndpointCard(
                        endpoint = endpoint,
                        unreadCount = endpoint.deviceId?.let { unreadCounts[it] } ?: 0,
                        onRequestConnection = { onRequestConnection(endpoint.id) },
                        onAcceptConnection = { onAcceptConnection(endpoint.id) },
                        onRejectConnection = { onRejectConnection(endpoint.id) },
                        onOpenChat = { endpoint.deviceId?.let { onContactClick(it) } }
                    )
                }

                if (contacts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Saved Contacts (${contacts.size})",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(contacts) { contact ->
                        ContactCard(
                            contact = contact,
                            isOnline = endpoints.any { it.deviceId == contact.deviceId && it.isConnected },
                            unreadCount = unreadCounts[contact.deviceId] ?: 0,
                            onContactClick = { onContactClick(contact.deviceId) }
                        )
                    }
                }
            }
        }
    }

    // ── Photo Editor Overlay ──────────────────────────────────────────────────
    editingBitmap?.let { bmp ->
        PhotoEditorScreen(
            sourceBitmap = bmp,
            onConfirm = { base64 ->
                AvatarStore.saveMyAvatar(context, base64)
                onAvatarChanged(base64)
                bmp.recycle()
                editingBitmap = null
            },
            onDismiss = {
                bmp.recycle()
                editingBitmap = null
            }
        )
    }
    } // end outer Box
}

// ── Contact Card ──────────────────────────────────────────────────────────────

@Composable
fun ContactCard(contact: ContactEntity, isOnline: Boolean, unreadCount: Int, onContactClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContactClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(
                name = contact.displayName,
                avatarBase64 = contact.avatarBase64,
                size = 44.dp,
                borderColor = if (isOnline) AccentNeon else Color.Transparent,
                borderWidth = if (isOnline) 2.dp else 0.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = contact.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isOnline) "● Online" else "○ Offline",
                    color = if (isOnline) AccentNeon else TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ErrorColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Endpoint Card ─────────────────────────────────────────────────────────────

@Composable
fun EndpointCard(
    endpoint: MeshEndpoint,
    unreadCount: Int,
    onRequestConnection: () -> Unit,
    onAcceptConnection: () -> Unit,
    onRejectConnection: () -> Unit,
    onOpenChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (endpoint.isConnected) onOpenChat()
                else if (!endpoint.connectionRequested) onRequestConnection()
            },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    name = endpoint.name,
                    avatarBase64 = endpoint.avatarBase64,
                    size = 44.dp,
                    borderColor = if (endpoint.isConnected) AccentPurple else Color.Transparent,
                    borderWidth = if (endpoint.isConnected) 2.dp else 0.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = endpoint.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (endpoint.isConnected) "● Connected" else "○ Discovered",
                        color = if (endpoint.isConnected) AccentPurple else TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ErrorColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (endpoint.connectionRequested && endpoint.authenticationToken != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundDark)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (endpoint.isIncomingRequest) "Incoming Connection" else "Verifying Connection",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verify this code on both devices: ${endpoint.authenticationToken}",
                        color = AccentNeon,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAcceptConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentNeon)
                        ) {
                            Text("Accept", color = BackgroundDark)
                        }
                        OutlinedButton(onClick = onRejectConnection) {
                            Text("Reject", color = ErrorColor)
                        }
                    }
                }
            }
        }
    }
}
