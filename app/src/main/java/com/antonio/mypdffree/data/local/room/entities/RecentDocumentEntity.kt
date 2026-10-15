package com.antonio.mypdffree.data.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * El URI es la clave natural: el mismo documento abierto dos veces actualiza su fila en vez de duplicarla.
 * Indice en last_opened_at porque toda lectura ordena por esa columna.
 */
@Entity(
    tableName = "recent_documents",
    indices = [Index(value = ["last_opened_at"])],
)
data class RecentDocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long?,
    @ColumnInfo(name = "page_count") val pageCount: Int,
    @ColumnInfo(name = "last_page_index") val lastPageIndex: Int,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long,
)
