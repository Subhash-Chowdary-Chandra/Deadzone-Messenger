package com.deadzone.messenger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.ui.platform.LocalContext
import com.deadzone.messenger.media.AudioRecorder
import com.deadzone.messenger.media.AudioPlayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deadzone.messenger.model.MeshMessage
import com.deadzone.messenger.ui.theme.*
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    title: String,
    messages: List<MeshMessage>,
    avatarBase64: String? = null,   // Peer/contact avatar (null for group chat)
    onSendMessage: (String) -> Unit,
    onSendMedia: (String, File) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onDeleteMessageLocally: (String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    
    // Media handling
    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var tempAudioFile by remember { mutableStateOf<File?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                val tempFile = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
                tempFile.outputStream().use { out -> inputStream.copyTo(out) }
                onSendMedia("IMAGE", tempFile)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarImage(
                        name = title,
                        avatarBase64 = avatarBase64,
                        size = 34.dp,
                        borderColor = AccentNeon,
                        borderWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, color = TextPrimary, fontSize = 16.sp)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )

        var fullScreenImage by remember { mutableStateOf<String?>(null) }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(messages.sortedByDescending { it.timestamp }) { msg ->
                MessageBubble(
                    message = msg, 
                    onDelete = onDeleteMessage,
                    onDeleteLocally = onDeleteMessageLocally,
                    onToggleSave = onToggleSave,
                    onImageClick = { path -> fullScreenImage = path }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // Full screen image viewer overlay
        if (fullScreenImage != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { fullScreenImage = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val coroutineScope = rememberCoroutineScope()
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AsyncImage(
                        model = File(fullScreenImage!!),
                        contentDescription = "Full Screen Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { 
                                coroutineScope.launch {
                                    com.deadzone.messenger.utils.MediaUtils.saveMediaToPublicStorage(context, File(fullScreenImage!!), "IMAGE")
                                }
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Download", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { fullScreenImage = null },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentNeon,
                    unfocusedBorderColor = CardDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentNeon
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            // Image Attachment Button
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardDark)
            ) {
                Icon(Icons.Default.Image, contentDescription = "Attach Image", tint = AccentNeon)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Audio Record Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isRecording) ErrorColor else CardDark)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isRecording = true
                                tempAudioFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.mp4")
                                audioRecorder.start(tempAudioFile!!)
                                tryAwaitRelease()
                                isRecording = false
                                audioRecorder.stop()
                                tempAudioFile?.let {
                                    if (it.exists() && it.length() > 0) {
                                        onSendMedia("AUDIO", it)
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Hold to Record", tint = TextPrimary)
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            // Send Text Button
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AccentPurple)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = TextPrimary)
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MeshMessage, 
    onDelete: (String) -> Unit,
    onDeleteLocally: (String) -> Unit,
    onToggleSave: (String, Boolean) -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    val isMe = message.isFromMe
    val isSaved = message.isSaved
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            if (!isMe) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderName,
                        color = if (isSaved) AccentPurple else AccentNeon,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    if (isSaved) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Bookmark, 
                            contentDescription = null, 
                            tint = AccentPurple, 
                            modifier = Modifier.size(10.dp).padding(bottom = 4.dp)
                        )
                        Text(
                            "Saved", 
                            color = AccentPurple, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            } else if (isSaved) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Saved", 
                        color = AccentPurple, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Bookmark, 
                        contentDescription = null, 
                        tint = AccentPurple, 
                        modifier = Modifier.size(10.dp).padding(bottom = 4.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 16.dp
                        )
                    )
                    .background(
                        if (isSaved) AccentPurple.copy(alpha = 0.15f)
                        else if (isMe) CardDarkOpaque else CardDark
                    )
                    .border(
                        width = if (isSaved) 2.dp else 0.dp,
                        color = if (isSaved) AccentPurple else Color.Transparent,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 16.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                showMenu = true
                            },
                            onTap = {
                                if (message.messageType == "IMAGE" && message.localFilePath != null) {
                                    onImageClick(message.localFilePath)
                                }
                            }
                        )
                    }
                    .padding(12.dp)
            ) {
                Column {
                    if (message.messageType == "IMAGE" && message.localFilePath != null) {
                        AsyncImage(
                            model = File(message.localFilePath),
                            contentDescription = "Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else if (message.messageType == "AUDIO" && message.localFilePath != null) {
                        val audioPlayer = remember { AudioPlayer() }
                        var isPlaying by remember { mutableStateOf(false) }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        audioPlayer.stop()
                                        isPlaying = false
                                    } else {
                                        audioPlayer.play(File(message.localFilePath)) {
                                            isPlaying = false
                                        }
                                        isPlaying = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Stop",
                                    tint = TextPrimary
                                )
                            }
                            Text("Voice Note", color = TextPrimary, fontSize = 16.sp)
                        }
                    } else {
                        Text(text = message.content, color = TextPrimary, fontSize = 16.sp)
                    }
                    
                    if (isMe) {
                        Row(modifier = Modifier.align(Alignment.End).padding(top = 4.dp)) {
                            if (!message.isSent) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Pending",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                val statusTint = if (message.isRead) Color(0xFF4CAF50) else TextSecondary
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Message Status",
                                    tint = statusTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Context Menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(CardDarkOpaque)
                ) {
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isSaved) Icons.Default.BookmarkBorder else Icons.Default.Bookmark, 
                                    contentDescription = null, 
                                    tint = AccentPurple, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isSaved) "Unsave Message" else "Save Message", color = TextPrimary)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onToggleSave(message.id, !isSaved)
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete for Me", color = ErrorColor)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDeleteLocally(message.id)
                        }
                    )

                    if (isMe) {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete for Everyone", color = ErrorColor)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete(message.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
