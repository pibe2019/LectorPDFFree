package com.antonio.mypdffree.data.pdf

import android.util.LruCache

/**
 * Cache LRU de paginas ya pintadas, medida en BYTES (no en numero de paginas: una pagina A4 y un plano
 * A0 no ocupan lo mismo). Evita repintar al volver atras con el scroll, que es lo mas habitual al leer.
 *
 * No es thread-safe por diseño (LruCache si lo es, pero get+put no son atomicos juntos):
 * solo se usa bajo el Mutex de [AndroidPdfDocumentHandle].
 */
internal class PageBitmapCache(maxBytes: Int = defaultMaxBytes()) {

    private data class Key(val pageIndex: Int, val widthPx: Int)

    private val cache = object : LruCache<Key, AndroidPageBitmap>(maxBytes) {
        override fun sizeOf(key: Key, value: AndroidPageBitmap): Int = value.bitmap.allocationByteCount
    }

    fun get(pageIndex: Int, widthPx: Int): AndroidPageBitmap? = cache.get(Key(pageIndex, widthPx))

    fun put(widthPx: Int, page: AndroidPageBitmap) {
        cache.put(Key(page.pageIndex, widthPx), page)
    }

    fun clear() = cache.evictAll()

    private companion object {
        const val MAX_CACHE_BYTES = 64 * 1024 * 1024

        /** 1/8 del heap de la app, con techo de 64 MB: la recomendacion clasica de Android para caches de imagen. */
        fun defaultMaxBytes(): Int =
            (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(MAX_CACHE_BYTES.toLong()).toInt()
    }
}
