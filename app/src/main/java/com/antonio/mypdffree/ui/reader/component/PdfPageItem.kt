package com.antonio.mypdffree.ui.reader.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.model.result.fold
import com.antonio.mypdffree.ui.core.mapper.asImageBitmap

/** Proporcion ancho/alto de un A4: mejor suposicion para el hueco de una pagina aun no pintada. */
internal const val A4_ASPECT_RATIO = 1f / 1.4142f

/** La hoja de un PDF es blanca siempre (tambien en tema oscuro): sus colores son fijos, no del tema. */
private val PaperColor = Color.White
private val OnPaperColor = Color(0xFF5F6368)

private sealed interface PageRenderState {
    data object Loading : PageRenderState
    @Immutable data class Ready(val image: ImageBitmap) : PageRenderState
    data object Failed : PageRenderState
}

@Composable
internal fun PdfPageItem(
    pageIndex: Int,
    pageCount: Int,
    renderWidthPx: Int,
    placeholderAspectRatio: Float,
    pageLoader: PageLoader,
    onAspectRatioKnown: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // produceState conserva el valor anterior al cambiar las claves: al subir el zoom de escalon se
    // sigue viendo la imagen actual (estirada) hasta que llega la nitida, sin parpadeo a "cargando".
    // La coroutine se cancela sola si la pagina sale de pantalla.
    val renderState by produceState<PageRenderState>(PageRenderState.Loading, pageIndex, renderWidthPx) {
        if (renderWidthPx <= 0) return@produceState
        pageLoader.load(pageIndex, renderWidthPx).fold(
            onSuccess = { page -> value = PageRenderState.Ready(page.asImageBitmap()) },
            // Si ya habia una imagen (fallo al re-pintar con mas zoom) se conserva: peor nitidez, no un hueco.
            onFailure = { if (value !is PageRenderState.Ready) value = PageRenderState.Failed },
        )
    }

    val ready = renderState as? PageRenderState.Ready
    val aspectRatio = ready?.let { it.image.width.toFloat() / it.image.height } ?: placeholderAspectRatio
    if (ready != null) {
        LaunchedEffect(aspectRatio) { onAspectRatioKnown(aspectRatio) }
    }

    val description = stringResource(R.string.cd_page_of, pageIndex + 1, pageCount)
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .background(PaperColor)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (val state = renderState) {
            PageRenderState.Loading -> CircularProgressIndicator(modifier = Modifier.size(32.dp), color = OnPaperColor)
            is PageRenderState.Ready -> Image(
                bitmap = state.image,
                contentDescription = null, // la descripcion ya esta en el contenedor
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            PageRenderState.Failed -> PageError()
        }
    }
}

@Composable
private fun PageError() {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = OnPaperColor)
        Text(
            text = stringResource(R.string.error_pdf_render_failed),
            color = OnPaperColor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
