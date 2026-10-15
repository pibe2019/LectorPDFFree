package com.antonio.mypdffree.ui.core.mapper

import androidx.compose.runtime.Composable
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.ui.core.util.UiText
import com.antonio.mypdffree.ui.core.util.asString

/** `when` exhaustivo SIN `else`: una variante nueva de DomainError no compila hasta tener su texto. */
fun DomainError.toUiText(): UiText.Resolved = when (this) {
    is DomainError.Pdf        -> toUiText()
    is DomainError.Local      -> toUiText()
    is DomainError.Validation -> toUiText()
    is DomainError.Unknown    -> UiText.StringResource(R.string.error_unknown)
}

fun DomainError.Pdf.toUiText(): UiText.Resolved = when (this) {
    DomainError.Pdf.NotFound          -> UiText.StringResource(R.string.error_pdf_not_found)
    DomainError.Pdf.AccessDenied      -> UiText.StringResource(R.string.error_pdf_access_denied)
    DomainError.Pdf.PasswordProtected -> UiText.StringResource(R.string.error_pdf_password_protected)
    DomainError.Pdf.Corrupted         -> UiText.StringResource(R.string.error_pdf_corrupted)
    DomainError.Pdf.RenderFailed      -> UiText.StringResource(R.string.error_pdf_render_failed)
}

fun DomainError.Local.toUiText(): UiText.Resolved = when (this) {
    DomainError.Local.Storage -> UiText.StringResource(R.string.error_local_storage)
}

fun DomainError.Validation.toUiText(): UiText.Resolved = when (this) {
    DomainError.Validation.PageNumberEmpty         -> UiText.StringResource(R.string.validation_page_empty)
    DomainError.Validation.PageNumberNotANumber    -> UiText.StringResource(R.string.validation_page_not_a_number)
    is DomainError.Validation.PageNumberOutOfRange -> UiText.StringResource(R.string.validation_page_out_of_range, listOf(pageCount))
}

@Composable
fun DomainError.asText(): String = toUiText().asString()
