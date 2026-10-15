package com.antonio.mypdffree.ui.reader.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Estado del zoom del lector. Es estado de PRESENTACION puro (como un LazyListState): vive en la UI
 * con rememberSaveable, no en el ViewModel; ninguna regla de negocio depende de el.
 *
 * Modelo: el zoom AGRANDA las paginas en el layout (ancho = viewport * [scale]) en vez de escalar la
 * lista con graphicsLayer. Asi el scroll vertical sigue siendo el nativo de LazyColumn (con fling) y
 * el desplazamiento horizontal es solo [offsetX], que se aplica en la fase de dibujo (sin recomponer).
 */
@Stable
class PdfZoomState(initialScale: Float = MIN_SCALE, initialOffsetX: Float = 0f) {

    var scale by mutableFloatStateOf(initialScale)
        private set

    /** Traslacion horizontal en px; siempre en [viewport - contenido, 0]. */
    var offsetX by mutableFloatStateOf(initialOffsetX)
        private set

    private var viewportWidthPx by mutableIntStateOf(0)

    val isZoomed: Boolean get() = scale > MIN_SCALE + ZOOM_EPSILON

    /**
     * Resolucion a la que conviene pintar: por escalones y no continua, porque repintar una pagina en
     * cada frame del pellizco seria carisimo. Entre escalones, la imagen actual se estira y se ve bien.
     */
    val renderScaleBucket: Int
        get() = when {
            scale < 1.5f -> 1
            scale < 3f -> 2
            else -> 3
        }

    fun onViewportChanged(widthPx: Int) {
        viewportWidthPx = widthPx
        offsetX = clampOffset(offsetX)
    }

    /**
     * Aplica un factor de zoom manteniendo fijo el punto bajo los dedos ([focusX]).
     * @return el factor REALMENTE aplicado (1f si se choco con un limite), para ajustar el scroll vertical.
     */
    fun zoomBy(factor: Float, focusX: Float): Float {
        val oldScale = scale
        val newScale = (oldScale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        if (newScale == oldScale) return 1f
        val applied = newScale / oldScale
        scale = newScale
        // El punto de contenido bajo el dedo estaba en (focusX - offsetX); tras escalar esta en ese valor * applied.
        offsetX = clampOffset(focusX - (focusX - offsetX) * applied)
        return applied
    }

    fun panBy(deltaX: Float) {
        offsetX = clampOffset(offsetX + deltaX)
    }

    private fun clampOffset(value: Float): Float {
        val minOffset = (viewportWidthPx - viewportWidthPx * scale).coerceAtMost(0f)
        return value.coerceIn(minOffset, 0f)
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2.5f
        private const val ZOOM_EPSILON = 0.01f

        val Saver = listSaver<PdfZoomState, Float>(
            save = { listOf(it.scale, it.offsetX) },
            restore = { PdfZoomState(initialScale = it[0], initialOffsetX = it[1]) },
        )
    }
}

@Composable
fun rememberPdfZoomState(): PdfZoomState = rememberSaveable(saver = PdfZoomState.Saver) { PdfZoomState() }
