package com.livecompose.livecapture.core.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoRecordDao {
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PhotoRecordEntity>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: String): PhotoRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoRecordEntity)

    @Delete
    suspend fun delete(entity: PhotoRecordEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: String)
}
