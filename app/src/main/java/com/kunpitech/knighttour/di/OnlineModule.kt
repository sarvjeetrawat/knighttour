package com.kunpitech.knighttour.di

import com.kunpitech.knighttour.data.online.OnlineSessionManager
import com.kunpitech.knighttour.data.repository.FirebaseGameRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ================================================================
//  ONLINE MODULE
//  File: di/OnlineModule.kt
//
//  Provides FirebaseGameRepository and OnlineSessionManager
//  as application-scoped singletons.
//
//  FirebaseGameRepository uses @Inject constructor so Hilt
//  creates it directly — no manual @Provides needed.
//  OnlineSessionManager also uses @Inject constructor.
//  Both are annotated @Singleton so one instance is shared
//  between LobbyViewModel and GameViewModel.
// ================================================================

@Module
@InstallIn(SingletonComponent::class)
object OnlineModule {

    // FirebaseGameRepository and OnlineSessionManager both use
    // @Inject constructor + @Singleton, so Hilt handles them
    // automatically without explicit @Provides bindings.
    //
    // This module is intentionally minimal — it exists as a
    // placeholder for any future Firebase configuration
    // (e.g. custom FirebaseApp instance, emulator config).
}