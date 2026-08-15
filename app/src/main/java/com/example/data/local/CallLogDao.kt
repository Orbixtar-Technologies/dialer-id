package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CallLogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogItem)

    @Delete
    suspend fun deleteCallLog(log: CallLogItem)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()
}
