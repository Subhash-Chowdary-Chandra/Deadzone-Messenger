package com.deadzone.messenger.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deadzone.messenger.model.MeshMessage

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val chatGroupId: String, // Used to group messages by chat (sender name or GROUP)
    val isSent: Boolean = true,
    val isSaved: Boolean = false,
    val isRead: Boolean = true,
    val messageType: String = "TEXT",
    val localFilePath: String? = null
)

fun MessageEntity.toMeshMessage() = MeshMessage(
    id = id,
    senderName = senderName,
    content = content,
    timestamp = timestamp,
    isFromMe = isFromMe,
    chatGroupId = chatGroupId,
    isSaved = isSaved,
    isSent = isSent,
    isRead = isRead,
    messageType = messageType,
    localFilePath = localFilePath
)

fun MeshMessage.toEntity(chatGroupId: String) = MessageEntity(
    id = id,
    senderName = senderName,
    content = content,
    timestamp = timestamp,
    isFromMe = isFromMe,
    chatGroupId = chatGroupId,
    isSaved = isSaved,
    isSent = isSent,
    isRead = isRead,
    messageType = messageType,
    localFilePath = localFilePath
)
