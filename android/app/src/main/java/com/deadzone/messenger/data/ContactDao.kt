package com.deadzone.messenger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts ORDER BY lastSeenTimestamp DESC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getContact(deviceId: String): ContactEntity?

    @Query("UPDATE contacts SET avatarBase64 = :avatarBase64 WHERE deviceId = :deviceId")
    suspend fun updateAvatar(deviceId: String, avatarBase64: String)
}
