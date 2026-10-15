package com.antonio.mypdffree.ui.reader.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.antonio.mypdffree.domain.model.PageBitmap
import com.antonio.mypdffree.domain.model.result.AppResult

/** Funcion que entrega una pagina pintada. La UI no sabe de use cases: solo recibe esto. */
fun interface PageLoader {
    suspend fun load(pageIndex: Int, widthPx: Int): AppResult<PageBitmap>
}

private val PageSpacing = 8.dp

@Composable
fun PdfPagesList(
    pageCount: Int,
    listState: LazyListState,
    pageLoader: PageLoader,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    zoomState: PdfZoomState = rememberPdfZoomState(),
) {
    // Proporcion de la ultima pagina pintada: los huecos de las siguientes la imitan y la lista
    // apenas "salta" al llegar las imagenes (la mayoria de PDFs tienen todas las paginas iguales).
    var placeholderAspectRatio by rememberSaveable { mutableFloatStateOf(A4_ASPECT_RATIO) }

    // BoxWithConstraints da el ancho YA en el primer frame. Con onSizeChanged el primer frame mediria
    // 0 px, las paginas tendrian alto 0 y la LazyColumn compondria el documento entero de golpe.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = constraints.maxWidth
        LaunchedEffect(viewportWidthPx) { zoomState.onViewportChanged(viewportWidthPx) }

        val renderWidthPx = viewportWidthPx * zoomState.renderScaleBucket
        val pageWidth = maxWidth * zoomState.scale

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pdfZoomGestures(zoomState, listState)
                // Arrastre horizontal con un dedo solo con zoom. Compose bloquea la orientacion por gesto:
                // el vertical va a la lista y el horizontal aqui, sin conflicto.
                .draggable(
                    state = rememberDraggableState { delta -> zoomState.panBy(delta) },
                    orientation = Orientation.Horizontal,
                    enabled = zoomState.isZoomed,
                ),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(PageSpacing),
        ) {
            items(count = pageCount, key = { it }, contentType = { PAGE_CONTENT_TYPE }) { pageIndex ->
                PdfPageItem(
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    renderWidthPx = renderWidthPx,
                    placeholderAspectRatio = placeholderAspectRatio,
                    pageLoader = pageLoader,
                    onAspectRatioKnown = { placeholderAspectRatio = it },
                    modifier = Modifier
                        // unbounded: la pagina puede ser mas ancha que la pantalla (zoom) y se alinea a la izquierda;
                        // offsetX (<= 0) la desplaza. Se lee en graphicsLayer: el paneo no recompone.
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .width(pageWidth)
                        .graphicsLayer { translationX = zoomState.offsetX },
                )
            }
        }
    }
}

private const val PAGE_CONTENT_TYPE = "pdf_page"
