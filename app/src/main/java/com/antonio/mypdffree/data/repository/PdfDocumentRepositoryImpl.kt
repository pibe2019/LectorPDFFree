package com.antonio.mypdffree.data.repository

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import com.antonio.mypdffree.data.datasource.device.DocumentContentDataSourceInterface
import com.antonio.mypdffree.data.error.DomainErrorException
import com.antonio.mypdffree.data.pdf.AndroidPdfDocumentHandle
import com.antonio.mypdffree.data.util.resultOf
import com.antonio.mypdffree.di.DefaultDispatcher
import com.antonio.mypdffree.di.IoDispatcher
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.domain.model.PdfDocumentHandle
import com.antonio.mypdffree.domain.model.PdfDocumentInfo
import com.antonio.mypdffree.domain.model.result.AppResult
import com.antonio.mypdffree.domain.repository.PdfDocumentRepositoryInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class PdfDocumentRepositoryImpl @Inject constructor(
    private val contentDataSource: DocumentContentDataSourceInterface,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    // Pintar es trabajo de CPU: va al pool Default, no al de IO.
    @param:DefaultDispatcher private val renderDispatcher: CoroutineDispatcher,
) : PdfDocumentRepositoryInterface {

    /**
     * NonCancellable: abrir es rapido, pero si la coroutine se cancelara justo al terminar, withContext
     * descartaria el handle ya creado y el descriptor quedaria abierto sin dueño. Asi el handle siempre
     * llega al llamador, que lo cierra si a su vez fue cancelado (ver OpenPdfDocumentUseCase).
     */
    override suspend fun open(uri: String): AppResult<PdfDocumentHandle> =
        withContext(ioDispatcher + NonCancellable) {
            resultOf {
                val androidUri = uri.toUri()
                // Metadatos ANTES de abrir el descriptor: si fallan, no hay nada que cerrar.
                val metadata = contentDataSource.queryMetadata(androidUri)
                val descriptor = contentDataSource.openReadOnly(androidUri)
                val renderer = createRenderer(descriptor)
                AndroidPdfDocumentHandle(
                    info = PdfDocumentInfo(
                        uri = uri,
                        displayName = metadata.displayName?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME,
                        sizeBytes = metadata.sizeBytes,
                        pageCount = renderer.pageCount,
                    ),
                    descriptor = descriptor,
                    renderer = renderer,
                    renderDispatcher = renderDispatcher,
                )
            }
        }

    /** Traduce los fallos de PdfRenderer, cuyo tipo de excepcion es ambiguo fuera de este contexto. */
    private fun createRenderer(descriptor: ParcelFileDescriptor): PdfRenderer {
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (e: SecurityException) {
            descriptor.close()
            throw DomainErrorException(DomainError.Pdf.PasswordProtected).apply { initCause(e) }
        } catch (e: IOException) {
            descriptor.close()
            throw DomainErrorException(DomainError.Pdf.Corrupted).apply { initCause(e) }
        }
        if (renderer.pageCount <= 0) {
            renderer.close()
            descriptor.close()
            throw DomainErrorException(DomainError.Pdf.Corrupted)
        }
        return renderer
    }

    private companion object {
        const val DEFAULT_NAME = "documento.pdf"
    }
}
