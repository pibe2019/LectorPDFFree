package com.antonio.mypdffree.data.datasource.device

import android.net.Uri
import android.os.ParcelFileDescriptor

/** Nombre y tamaño que reporta el proveedor del documento. */
data class DocumentMetadata(val displayName: String?, val sizeBytes: Long?)

/** Acceso a documentos via ContentResolver (Storage Access Framework). Llamadas bloqueantes: IPC con el proveedor. */
interface DocumentContentDataSourceInterface {
    fun queryMetadata(uri: Uri): DocumentMetadata
    /** Lanza FileNotFoundException / SecurityException; el repositorio las traduce. */
    fun openReadOnly(uri: Uri): ParcelFileDescriptor
    /** `false` si el proveedor no ofrecio un permiso persistible para este URI. */
    fun takePersistableReadPermission(uri: Uri): Boolean
    fun releasePersistableReadPermission(uri: Uri)
}
