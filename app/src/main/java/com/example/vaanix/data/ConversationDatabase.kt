package com.example.vaanix.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Entity(tableName = "conversation_messages")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val isFromUser: Boolean,
    val text: String,
    val languageTag: String,
    val actionStatusNote: String? = null,
    val isMockResponse: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_messages ORDER BY timestampMillis ASC, id ASC")
    fun observeMessages(): Flow<List<ConversationMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationMessageEntity): Long

    @Query("DELETE FROM conversation_messages")
    suspend fun clearAll()
}

@Database(
    entities = [ConversationMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaaniDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: VaaniDatabase? = null

        fun getInstance(context: Context): VaaniDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaaniDatabase::class.java,
                    "vaanix_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

interface ConversationRepository {
    val messages: Flow<List<ConversationMessageEntity>>
    suspend fun addMessage(message: ConversationMessageEntity)
    suspend fun clearHistory()
}

class RoomConversationRepository(
    private val dao: ConversationDao
) : ConversationRepository {
    override val messages: Flow<List<ConversationMessageEntity>> = dao.observeMessages()

    override suspend fun addMessage(message: ConversationMessageEntity) {
        dao.insert(message)
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }
}

class InMemoryConversationRepository : ConversationRepository {
    private val _messages = MutableStateFlow<List<ConversationMessageEntity>>(emptyList())
    private var nextId = 1L

    override val messages: Flow<List<ConversationMessageEntity>> = _messages.asStateFlow()

    override suspend fun addMessage(message: ConversationMessageEntity) {
        _messages.update { list -> list + message.copy(id = nextId++) }
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
