package com.antonio.mypdffree.di

import android.content.Context
import androidx.room.Room
import com.antonio.mypdffree.data.local.room.dao.RecentDocumentDao
import com.antonio.mypdffree.data.local.room.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataBaseModule {
    /** Tambien referenciado en backup_rules.xml y data_extraction_rules.xml para excluirlo del backup. */
    private const val DATABASE_NAME = "my_pdf_free.db"

    /** @Singleton: una conexion por proceso; dos instancias de Room sobre el mismo archivo se pisan. */
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideRecentDocumentDao(database: AppDatabase): RecentDocumentDao = database.recentDocumentDao()
}
