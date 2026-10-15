package com.antonio.mypdffree.data.pdf

import android.graphics.Bitmap
import com.antonio.mypdffree.domain.model.PageBitmap

/** Implementacion Android de [PageBitmap]. Solo la abre `ui/core/mapper/PageBitmapExt.kt`. */
class AndroidPageBitmap(
    override val pageIndex: Int,
    val bitmap: Bitmap,
) : PageBitmap {
    override val widthPx: Int get() = bitmap.width
    override val heightPx: Int get() = bitmap.height
}
