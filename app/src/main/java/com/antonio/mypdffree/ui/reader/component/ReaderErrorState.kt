package com.antonio.mypdffree.ui.reader.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.ui.core.mapper.asText

/** Error bloqueante (el documento no se abrio): pantalla completa con salida clara, no un snackbar. */
@Composable
fun ReaderErrorState(
    error: DomainError,
    canRemoveFromRecents: Boolean,
    onBackClick: () -> Unit,
    onRemoveFromRecentsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(R.string.reader_error_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = error.asText(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onBackClick) { Text(stringResource(R.string.action_back)) }
        if (canRemoveFromRecents) {
            OutlinedButton(onClick = onRemoveFromRecentsClick) { Text(stringResource(R.string.home_remove_from_recents)) }
        }
    }
}
