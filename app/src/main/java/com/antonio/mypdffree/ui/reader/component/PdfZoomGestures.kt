package com.antonio.mypdffree.ui.reader.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/**
 * Pellizco (2 dedos) y doble toque para el zoom.
 *
 * Se escucha en la pasada `Initial` (padre antes que hijos): con dos dedos el pellizco se consume
 * ANTES de que la LazyColumn lo interprete como scroll. Con un dedo no se consume nada y el scroll
 * vertical nativo funciona igual.
 */
internal fun Modifier.pdfZoomGestures(zoomState: PdfZoomState, listState: LazyListState): Modifier = this
    .pointerInput(zoomState, listState) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    handlePinch(event, zoomState, listState)
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .pointerInput(zoomState, listState) {
        detectTapGestures(
            onDoubleTap = { tap ->
                val target = if (zoomState.isZoomed) PdfZoomState.MIN_SCALE else PdfZoomState.DOUBLE_TAP_SCALE
                val applied = zoomState.zoomBy(target / zoomState.scale, tap.x)
                listState.keepVerticalFocus(applied, focusY = tap.y)
            },
        )
    }

private fun handlePinch(event: PointerEvent, zoomState: PdfZoomState, listState: LazyListState) {
    val centroid = event.calculateCentroid(useCurrent = true)
    val pan = event.calculatePan()
    val applied = zoomState.zoomBy(event.calculateZoom(), centroid.x)
    zoomState.panBy(pan.x)
    if (applied != 1f) {
        listState.keepVerticalFocus(applied, focusY = centroid.y, extraScrollPx = -pan.y)
    } else {
        // Sin cambio de zoom, arrastrar con dos dedos desplaza la lista como con uno.
        listState.dispatchRawDelta(-pan.y)
    }
}

/**
 * Tras escalar por [factor], recoloca el scroll para que el contenido bajo [focusY] no "salte".
 * Se ancla en el primer item visible: su desplazamiento interno escala con el (la separacion entre
 * paginas no escala; el error es de unos pocos px y es imperceptible).
 * `requestScrollToItem` (no suspend) aplica la posicion en el mismo frame que el nuevo tamaño.
 */
private fun LazyListState.keepVerticalFocus(factor: Float, focusY: Float, extraScrollPx: Float = 0f) {
    if (factor == 1f) return
    val anchorDistance = firstVisibleItemScrollOffset + focusY
    val newOffset = anchorDistance * factor - focusY - extraScrollPx
    requestScrollToItem(firstVisibleItemIndex, newOffset.roundToInt().coerceAtLeast(0))
}
