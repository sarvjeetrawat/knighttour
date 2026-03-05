package com.kunpitech.knighttour.di

import com.kunpitech.knighttour.data.repository.GameRepository
import com.kunpitech.knighttour.data.repository.GameRepositoryImpl
import com.kunpitech.knighttour.data.repository.ScoreRepository
import com.kunpitech.knighttour.data.repository.ScoreRepositoryImpl
import com.kunpitech.knighttour.data.repository.UserPreferencesRepository
import com.kunpitech.knighttour.data.repository.UserPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ===============================================================
//  REPOSITORY MODULE
//
//  Binds interfaces → implementations so ViewModels and
//  UseCases depend only on interfaces (easy to fake in tests).
//
//  Package: di/RepositoryModule.kt
// ===============================================================

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGameRepository(
        impl: GameRepositoryImpl,
    ): GameRepository

    @Binds
    @Singleton
    abstract fun bindScoreRepository(
        impl: ScoreRepositoryImpl,
    ): ScoreRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository
}