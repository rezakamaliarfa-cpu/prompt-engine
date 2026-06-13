package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- HISTORY ENTITY ---
@Entity(tableName = "prompt_history")
data class PromptHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val inputPrompt: String,
    val optimizedPrompt: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String,
    val agentName: String,
    val tone: String,
    val isOffline: Boolean
)

@Dao
interface PromptHistoryDao {
    @Query("SELECT * FROM prompt_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<PromptHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PromptHistory)

    @Query("DELETE FROM prompt_history WHERE id = :id")
    suspend fun deleteHistory(id: Int)

    @Query("DELETE FROM prompt_history")
    suspend fun clearHistory()
}

// --- CUSTOM INSTRUCTIONS ENTITY ---
@Entity(tableName = "custom_instructions")
data class CustomInstruction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val tone: String,
    val instruction: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CustomInstructionDao {
    @Query("SELECT * FROM custom_instructions ORDER BY timestamp DESC")
    fun getAllInstructions(): Flow<List<CustomInstruction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruction(instruction: CustomInstruction)

    @Query("DELETE FROM custom_instructions WHERE id = :id")
    suspend fun deleteInstruction(id: Int)
}

// --- SECURITY LOGS ENTITY ---
@Entity(tableName = "security_logs")
data class SecurityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val encryptionType: String = "AES-256-GCM"
)

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SecurityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLog)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}
