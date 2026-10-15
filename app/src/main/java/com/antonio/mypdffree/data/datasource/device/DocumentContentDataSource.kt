package com.antonio.mypdffree.data.datasource.device

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * La app NO pide permisos de almacenamiento: el usuario elige el archivo en el selector del sistema
 * (ACTION_OPEN_DOCUMENT) o lo abre desde otra app (ACTION_VIEW), y el sistema concede acceso a ese URI.
 * Es lo que exige la politica de Google Play; MANAGE_EXTERNAL_STORAGE seria motivo de rechazo.
 */
class DocumentContentDataSource @Inject constructor(
    private val contentResolver: ContentResolver,
) : DocumentContentDataSourceInterface {

    override fun queryMetadata(uri: Uri): DocumentMetadata {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            DocumentMetadata(
                displayName = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else null,
                // SIZE puede ser null: el proveedor no siempre lo conoce (p. ej. un archivo en la nube aun sin bajar).
                sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null,
            )
        } ?: DocumentMetadata(displayName = uri.lastPathSegment, sizeBytes = null)
    }

    override fun openReadOnly(uri: Uri): ParcelFileDescriptor =
        contentResolver.openFileDescriptor(uri, "r")
            ?: throw FileNotFoundException("El proveedor no devolvio descriptor para $uri")

    override fun takePersistableReadPermission(uri: Uri): Boolean =
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: SecurityException) {
            // No es un error: es la respuesta "este origen no permite retener acceso". Se modela como valor.
            false
        }

    override fun releasePersistableReadPermission(uri: Uri) {
        contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
