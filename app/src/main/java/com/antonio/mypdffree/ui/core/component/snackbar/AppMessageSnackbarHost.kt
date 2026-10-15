package com.antonio.mypdffree.ui.core.component.snackbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.antonio.mypdffree.ui.core.util.asString
import com.antonio.mypdffree.ui.modelui.AppMessage
import com.antonio.mypdffree.ui.modelui.UserMessage

@Composable
fun AppMessageSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val appMessage = (data.visuals as? AppMessageVisuals)?.appMessage
        val (container, content) = when (appMessage) {
            is AppMessage.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            is AppMessage.Info, null -> MaterialTheme.colorScheme.inverseSurface to MaterialTheme.colorScheme.inverseOnSurface
        }
        Snackbar(snackbarData = data, containerColor = container, contentColor = content, actionColor = content)
    }
}

/**
 * Muestra el primer aviso pendiente y confirma al ViewModel que ya se vio. Centralizado para que
 * ninguna pantalla olvide el acuse de recibo (si se olvida, el aviso se repetiria tras rotar).
 */
@Composable
fun UserMessagesEffect(
    messages: List<UserMessage>,
    hostState: SnackbarHostState,
    onMessageShown: (Long) -> Unit,
) {
    val message = messages.firstOrNull() ?: return
    val text = message.message.text.asString()
    LaunchedEffect(message.id) {
        hostState.showSnackbar(
            AppMessageVisuals(
                message = text,
                appMessage = message.message,
                duration = if (message.message is AppMessage.Error) SnackbarDuration.Long else SnackbarDuration.Short,
            )
        )
        onMessageShown(message.id)
    }
}

private class AppMessageVisuals(
    override val message: String,
    val appMessage: AppMessage,
    override val duration: SnackbarDuration,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
) : SnackbarVisuals
