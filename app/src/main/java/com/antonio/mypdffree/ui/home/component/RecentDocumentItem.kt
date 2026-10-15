package com.antonio.mypdffree.ui.home.component

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.model.RecentDocument

@Composable
fun RecentDocumentItem(
    document: RecentDocument,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.cd_open_document), onClick = onClick),
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_pdf_document),
                contentDescription = null, // decorativo: el nombre ya identifica la fila
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = {
            Text(document.displayName, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
        },
        supportingContent = { Text(document.supportingText(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = { RecentItemMenu(documentName = document.displayName, onRemoveClick = onRemoveClick) },
    )
}

/**
 * "Pagina 3 de 12 · 2,4 MB · hace 5 minutos". Tamaño y tiempo relativo se formatean con las APIs del
 * sistema: respetan el idioma del dispositivo y la convencion de unidades (KB/MB, coma o punto).
 */
@Composable
private fun RecentDocument.supportingText(): String {
    val context = LocalContext.current
    val parts = buildList {
        add(stringResource(R.string.home_recent_page_of, lastPageIndex + 1, pageCount))
        sizeBytes?.let { add(Formatter.formatShortFileSize(context, it)) }
        if (lastOpenedAtMillis > 0) {
            add(
                DateUtils.getRelativeTimeSpanString(
                    lastOpenedAtMillis,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                ).toString()
            )
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun RecentItemMenu(documentName: String, onRemoveClick: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                // Con el nombre: TalkBack distingue el boton de cada fila ("Opciones de Contrato.pdf").
                contentDescription = stringResource(R.string.cd_document_options, documentName),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_remove_from_recents)) },
                onClick = {
                    expanded = false
                    onRemoveClick()
                },
            )
        }
    }
}
