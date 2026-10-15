package com.antonio.mypdffree.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/*
 * Los dispatchers se inyectan, nunca se escriben a mano (Dispatchers.IO) en una clase:
 * en los tests se sustituyen por un TestDispatcher y el codigo se vuelve determinista.
 * No hay @MainDispatcher: ninguna clase lo necesita (los ViewModels usan viewModelScope).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    /** Bloqueo por E/S: archivos, ContentResolver (IPC). Pool grande (64 hilos). */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** CPU: pintar paginas de PDF. Tantos hilos como nucleos. */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
