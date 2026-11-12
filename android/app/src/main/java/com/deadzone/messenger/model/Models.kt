package com.deadzone.messenger.model

data class MeshEndpoint(
    val id: String,
    val name: String,
    val deviceId: String? = null,
    val isConnected: Boolean = false,
    val authenticationToken: String? = null,
    val connectionRequested: Boolean = false,
    val isIncomingRequest: Boolean = false,
    val avatarBase64: String? = null
)

data class MeshMessage(
    val id: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val chatGroupId: String = "",
    val isSaved: Boolean = false,
    val isSent: Boolean = true,
    val isRead: Boolean = true,
    val messageType: String = "TEXT",
    val localFilePath: String? = null
)
