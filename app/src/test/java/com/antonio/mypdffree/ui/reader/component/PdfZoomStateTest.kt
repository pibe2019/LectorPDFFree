package com.antonio.mypdffree.ui.reader.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PdfZoomStateTest {

    private lateinit var zoom: PdfZoomState

    @Before
    fun setUp() {
        zoom = PdfZoomState().apply { onViewportChanged(VIEWPORT) }
    }

    @Test
    fun `el zoom mantiene fijo el punto bajo los dedos`() {
        val applied = zoom.zoomBy(2f, focusX = 500f)

        assertEquals(2f, applied, DELTA)
        // El punto de contenido x=500 pasa a x=1000; para seguir en pantalla en 500 el offset es -500.
        assertEquals(-500f, zoom.offsetX, DELTA)
    }

    @Test
    fun `el zoom se limita a MAX_SCALE y devuelve el factor realmente aplicado`() {
        val applied = zoom.zoomBy(10f, focusX = 0f)

        assertEquals(PdfZoomState.MAX_SCALE, zoom.scale, DELTA)
        assertEquals(PdfZoomState.MAX_SCALE, applied, DELTA)
        assertEquals(1f, zoom.zoomBy(2f, focusX = 0f), DELTA) // ya en el tope: no cambia nada
    }

    @Test
    fun `sin zoom no hay desplazamiento horizontal posible`() {
        zoom.panBy(300f)
        assertEquals(0f, zoom.offsetX, DELTA)
        zoom.panBy(-300f)
        assertEquals(0f, zoom.offsetX, DELTA)
        assertFalse(zoom.isZoomed)
    }

    @Test
    fun `con zoom el desplazamiento no deja ver fuera de la pagina`() {
        zoom.zoomBy(2f, focusX = 0f) // contenido de 2000 px en un viewport de 1000

        zoom.panBy(-5_000f)
        assertEquals(-1_000f, zoom.offsetX, DELTA)
        zoom.panBy(5_000f)
        assertEquals(0f, zoom.offsetX, DELTA)
        assertTrue(zoom.isZoomed)
    }

    @Test
    fun `al volver a escala 1 el desplazamiento se reinicia`() {
        zoom.zoomBy(3f, focusX = 800f)
        zoom.zoomBy(1f / 3f, focusX = 200f)

        assertEquals(1f, zoom.scale, DELTA)
        assertEquals(0f, zoom.offsetX, DELTA)
    }

    @Test
    fun `la resolucion de pintado sube por escalones`() {
        assertEquals(1, zoom.renderScaleBucket)
        zoom.zoomBy(1.6f, focusX = 0f)
        assertEquals(2, zoom.renderScaleBucket)
        zoom.zoomBy(2f, focusX = 0f) // 3.2
        assertEquals(3, zoom.renderScaleBucket)
    }

    @Test
    fun `un viewport mas estrecho re-ajusta el desplazamiento`() {
        zoom.zoomBy(2f, focusX = 1_000f) // offset -1000 (tope)
        zoom.onViewportChanged(500)       // ahora el tope es 500 - 1000 = -500

        assertEquals(-500f, zoom.offsetX, DELTA)
    }

    private companion object {
        const val VIEWPORT = 1_000
        const val DELTA = 0.001f
    }
}
