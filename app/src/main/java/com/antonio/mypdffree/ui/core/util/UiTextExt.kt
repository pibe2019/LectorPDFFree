package com.antonio.mypdffree.ui.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.antonio.mypdffree.ui.core.mapper.asText

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> stringResource(resId, *args.toTypedArray())
    is UiText.FromError -> error.asText()
}
