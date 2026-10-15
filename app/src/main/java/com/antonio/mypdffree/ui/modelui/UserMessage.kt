package com.antonio.mypdffree.ui.modelui

import java.util.concurrent.atomic.AtomicLong

/**
 * Aviso con identidad. Los eventos de una sola vez se modelan como ESTADO con acuse de recibo
 * (la UI lo muestra y llama a `onMessageShown(id)`), nunca como Channel: un Channel pierde el evento
 * si llega mientras la UI no esta suscrita (rotacion, segundo plano).
 */
data class UserMessage(val id: Long, val message: AppMessage) {
    companion object {
        private val counter = AtomicLong(0)
        fun of(message: AppMessage) = UserMessage(counter.incrementAndGet(), message)
    }
}
