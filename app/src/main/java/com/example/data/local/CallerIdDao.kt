package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CallerIdItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CallerIdDao {
    @Query("SELECT * FROM caller_ids ORDER BY isPrimary DESC, createdAt DESC")
    fun getAllCallerIds(): Flow<List<CallerIdItem>>

    @Query("SELECT * FROM caller_ids WHERE id = :id LIMIT 1")
    suspend fun getCallerIdById(id: String): CallerIdItem?

    @Query("SELECT * FROM caller_ids WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryCallerId(): CallerIdItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallerId(item: CallerIdItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CallerIdItem>)

    @Update
    suspend fun updateCallerId(item: CallerIdItem)

    @Query("UPDATE caller_ids SET isPrimary = 0")
    suspend fun clearPrimaryFlags()

    @Query("UPDATE caller_ids SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimary(id: String)

    @Delete
    suspend fun deleteCallerId(item: CallerIdItem)

    @Query("DELETE FROM caller_ids WHERE id = :id")
    suspend fun deleteById(id: String)
}
