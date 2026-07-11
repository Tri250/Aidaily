package com.livecompose.livecapture.core.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PhotoRecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoRecordDao(): PhotoRecordDao
}
