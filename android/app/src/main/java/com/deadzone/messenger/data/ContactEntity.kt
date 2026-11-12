package com.deadzone.messenger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val deviceId: String,
    val displayName: String,
    val lastSeenTimestamp: Long,
    val avatarBase64: String? = null,
    val publicKey: String? = null
)
