package com.antonio.mypdffree.ui.core.mapper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.antonio.mypdffree.data.pdf.AndroidPageBitmap
import com.antonio.mypdffree.domain.model.PageBitmap

/**
 * UNICA costura ui -> data, deliberada (mismo caso que `GpsResolutionExt` en GuiaMedicaSebas).
 * El dominio transporta la pagina como tipo opaco; aqui, ya en Android, se abre para pintarla.
 * `asImageBitmap()` no copia pixeles: envuelve el mismo Bitmap.
 */
fun PageBitmap.asImageBitmap(): ImageBitmap = (this as AndroidPageBitmap).bitmap.asImageBitmap()
