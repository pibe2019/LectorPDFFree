package com.antonio.mypdffree.domain.model

/**
 * Imagen ya pintada de una pagina, OPACA para el dominio: la implementacion (data) envuelve un
 * `android.graphics.Bitmap`. Mismo patron que `GpsResolution` en GuiaMedicaSebas: el dominio la
 * transporta sin conocerla y la UI la abre en una unica costura documentada (`PageBitmapExt`).
 */
interface PageBitmap {
    val pageIndex: Int
    val widthPx: Int
    val heightPx: Int
}
