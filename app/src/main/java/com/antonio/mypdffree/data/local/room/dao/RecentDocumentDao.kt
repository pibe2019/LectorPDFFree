package com.antonio.mypdffree.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.antonio.mypdffree.data.local.room.entities.RecentDocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Clase abstracta (y no interfaz) para poder tener metodos @Transaction con cuerpo:
 * "leer los URIs y borrarlos" tiene que ser atomico o se liberarian permisos de filas que no se borraron.
 * El desempate por uri hace el orden determinista cuando dos aperturas caen en el mismo milisegundo.
 */
@Dao
abstract class RecentDocumentDao {

    @Query("SELECT * FROM recent_documents ORDER BY last_opened_at DESC, uri ASC")
    abstract fun observeAll(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents WHERE uri = :uri")
    abstract suspend fun getByUri(uri: String): RecentDocumentEntity?

    @Upsert
    abstract suspend fun upsert(entity: RecentDocumentEntity)

    @Query("UPDATE recent_documents SET last_page_index = :pageIndex WHERE uri = :uri")
    abstract suspend fun updateLastPage(uri: String, pageIndex: Int): Int

    @Query("DELETE FROM recent_documents WHERE uri = :uri")
    abstract suspend fun deleteByUri(uri: String): Int

    @Query("SELECT uri FROM recent_documents")
    protected abstract suspend fun getAllUris(): List<String>

    @Query("DELETE FROM recent_documents")
    protected abstract suspend fun deleteAll()

    /** `LIMIT -1` = sin limite en SQLite: todo lo que queda despues de los [keep] primeros. */
    @Query("SELECT uri FROM recent_documents ORDER BY last_opened_at DESC, uri ASC LIMIT -1 OFFSET :keep")
    protected abstract suspend fun getUrisBeyond(keep: Int): List<String>

    @Query("DELETE FROM recent_documents WHERE uri IN (:uris)")
    protected abstract suspend fun deleteByUris(uris: List<String>)

    @Transaction
    open suspend fun deleteAllReturningUris(): List<String> {
        val uris = getAllUris()
        deleteAll()
        return uris
    }

    @Transaction
    open suspend fun trimTo(keep: Int): List<String> {
        val uris = getUrisBeyond(keep)
        if (uris.isNotEmpty()) deleteByUris(uris)
        return uris
    }
}
