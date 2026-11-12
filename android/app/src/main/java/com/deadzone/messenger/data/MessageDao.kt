package com.deadzone.messenger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class UnreadCount(
    val chatGroupId: String,
    val count: Int
)

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE chatGroupId = :chatGroupId ORDER BY timestamp DESC")
    fun getMessagesForChat(chatGroupId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE timestamp < :thresholdTime AND isSaved = 0")
    suspend fun deleteMessagesOlderThan(thresholdTime: Long)

    @Query("SELECT * FROM messages WHERE isSent = 0 AND chatGroupId = :chatGroupId ORDER BY timestamp ASC")
    suspend fun getUnsentMessages(chatGroupId: String): List<MessageEntity>

    @Query("UPDATE messages SET isSent = 1 WHERE id = :messageId")
    suspend fun markMessageAsSent(messageId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("UPDATE messages SET isSaved = :isSaved WHERE id = :messageId")
    suspend fun updateSavedStatus(messageId: String, isSaved: Boolean)

    @Query("SELECT chatGroupId, COUNT(id) as count FROM messages WHERE isRead = 0 AND isFromMe = 0 GROUP BY chatGroupId")
    fun getUnreadCounts(): Flow<List<UnreadCount>>

    @Query("UPDATE messages SET isRead = 1 WHERE chatGroupId = :chatGroupId AND isRead = 0")
    suspend fun markMessagesAsRead(chatGroupId: String)
}
