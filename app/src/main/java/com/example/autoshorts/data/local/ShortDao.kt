package com.example.autoshorts.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortDao {
    @Query("SELECT * FROM generated_shorts ORDER BY createdAtTimestamp DESC")
    fun getAllShorts(): Flow<List<ShortEntity>>

    @Query("SELECT * FROM generated_shorts WHERE id = :id LIMIT 1")
    suspend fun getShortById(id: Long): ShortEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShort(short: ShortEntity): Long

    @Query("DELETE FROM generated_shorts WHERE id = :id")
    suspend fun deleteShortById(id: Long)

    @Query("DELETE FROM generated_shorts")
    suspend fun deleteAllShorts()
}
