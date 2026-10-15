package com.antonio.mypdffree.ui.core.navigation.ex

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/*
 * Anti doble-toque por IDENTIDAD de destino, no por tiempo: si la cima ya es esa pantalla no se apila
 * otra. Evita el estado global mutable (un `lastNavTime` a nivel de archivo) y no descarta
 * navegaciones legitimas que llegan rapido.
 */
fun NavBackStack<NavKey>.navigateTo(screen: NavKey) {
    if (lastOrNull() != screen) add(screen)
}

/** `false` si no habia a donde volver: el llamador decide (normalmente, cerrar la app). */
fun NavBackStack<NavKey>.back(): Boolean =
    if (size > 1) {
        removeAt(lastIndex)
        true
    } else {
        false
    }
