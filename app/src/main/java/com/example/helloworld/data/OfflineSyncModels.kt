package com.example.helloworld.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    FAILED
}

@Entity(tableName = "local_chat_rooms")
data class LocalChatRoomEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String
)

@Entity(tableName = "local_chat_messages")
data class LocalChatMessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val message: String,
    val createdAt: String,
    val senderUsername: String?,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM local_chat_rooms")
    fun getAllRooms(): Flow<List<LocalChatRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<LocalChatRoomEntity>)

    @Query("SELECT * FROM local_chat_messages WHERE roomId = :roomId ORDER BY createdAt ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<LocalChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalChatMessageEntity>)

    @Query("SELECT * FROM local_chat_messages WHERE syncStatus = 'PENDING_INSERT'")
    suspend fun getPendingInserts(): List<LocalChatMessageEntity>

    @Query("UPDATE local_chat_messages SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM local_chat_messages WHERE id = :oldId")
    suspend fun deleteMessageById(oldId: String)
}

@Database(entities = [LocalChatRoomEntity::class, LocalChatMessageEntity::class], version = 1, exportSchema = false)
@TypeConverters(SyncStatusConverter::class)
abstract class AppLocalDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppLocalDatabase? = null

        fun getDatabase(context: Context): AppLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDatabase::class.java,
                    "kfcc_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SyncStatusConverter {
    @TypeConverter
    fun toStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: SyncStatus): String = status.name
}
