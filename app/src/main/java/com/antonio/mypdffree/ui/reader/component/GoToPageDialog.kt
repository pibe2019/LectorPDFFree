package com.antonio.mypdffree.ui.reader.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.antonio.mypdffree.R
import com.antonio.mypdffree.domain.error.DomainError
import com.antonio.mypdffree.ui.core.mapper.asText

@Composable
fun GoToPageDialog(
    input: String,
    pageCount: Int,
    error: DomainError.Validation?,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    // Foco automatico: el dialogo existe para escribir un numero; abrir el teclado ahorra un toque.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_go_to_page)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.focusRequester(focusRequester),
                label = { Text(stringResource(R.string.reader_go_to_page_label, pageCount)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it.asText()) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onConfirm() }),
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_go)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
