package com.antonio.mypdffree.di

import android.content.ContentResolver
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/**
 * Se inyecta ContentResolver y no Context: el data source declara exactamente lo que usa
 * y en un test se sustituye sin simular un Context entero.
 */
@Module
@InstallIn(SingletonComponent::class)
object DeviceModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver
}
