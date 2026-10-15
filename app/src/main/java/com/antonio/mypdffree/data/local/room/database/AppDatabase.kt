package com.antonio.mypdffree.data.local.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antonio.mypdffree.data.local.room.dao.RecentDocumentDao
import com.antonio.mypdffree.data.local.room.entities.RecentDocumentEntity

/**
 * A diferencia de GuiaMedicaSebas, esta BD NO es cache de un servidor: el historial y la pagina donde
 * se quedo el usuario solo existen aqui. Por eso no se usa fallbackToDestructiveMigration: cada cambio
 * de version lleva su Migration (o AutoMigration) apoyada en los esquemas exportados en app/schemas/.
 */
@Database(
    entities = [RecentDocumentEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentDocumentDao(): RecentDocumentDao
}
