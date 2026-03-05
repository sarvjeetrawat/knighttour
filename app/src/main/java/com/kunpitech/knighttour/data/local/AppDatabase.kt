package com.kunpitech.knighttour.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kunpitech.knighttour.data.local.dao.GameSessionDao
import com.kunpitech.knighttour.data.local.dao.ScoreDao
import com.kunpitech.knighttour.data.local.entity.GameSessionConverters
import com.kunpitech.knighttour.data.local.entity.GameSessionEntity
import com.kunpitech.knighttour.data.local.entity.ScoreEntity

// ===============================================================
//  APP DATABASE
//
//  Single Room database for the app.
//  Increment [version] and add a Migration whenever the schema
//  changes — never use fallbackToDestructiveMigration in prod.
//
//  Package: data/local/AppDatabase.kt
// ===============================================================

@Database(
    entities = [
        GameSessionEntity::class,
        ScoreEntity::class,
    ],
    version    = 1,
    exportSchema = true,   // saves schema JSON to /schemas — add to version control
)
@TypeConverters(GameSessionConverters::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun gameSessionDao(): GameSessionDao
    abstract fun scoreDao(): ScoreDao

    companion object {
        const val DATABASE_NAME = "knight_tour.db"
    }
}