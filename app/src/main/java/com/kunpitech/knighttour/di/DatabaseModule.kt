package com.kunpitech.knighttour.di

import android.content.Context
import androidx.room.Room
import com.kunpitech.knighttour.data.local.AppDatabase
import com.kunpitech.knighttour.data.local.dao.GameSessionDao
import com.kunpitech.knighttour.data.local.dao.ScoreDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ===============================================================
//  DATABASE MODULE
//
//  Provides the Room database and its DAOs as singletons.
//  Package: di/DatabaseModule.kt
// ===============================================================

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        // Add migrations here when schema changes in future versions:
        // .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()

    @Provides
    @Singleton
    fun provideGameSessionDao(db: AppDatabase): GameSessionDao =
        db.gameSessionDao()

    @Provides
    @Singleton
    fun provideScoreDao(db: AppDatabase): ScoreDao =
        db.scoreDao()
}