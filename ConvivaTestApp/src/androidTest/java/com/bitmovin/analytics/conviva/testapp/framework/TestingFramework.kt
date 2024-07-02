package com.bitmovin.analytics.conviva.testapp.framework

import android.os.Handler
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventEmitter
import com.bitmovin.player.api.event.on
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val BITMOVIN_PLAYER_LICENSE_KEY = "YOUR_LICENSE_KEY"
const val CONVIVA_CUSTOMER_KEY = "YOUR-CUSTOMER-KEY"
const val CONVIVA_GATEWAY_URL = "YOUR-GATEWAY-URL"


/**
 * Subscribes to an [Event] on the [Player] and suspends until the event is emitted.
 * Optionally a [condition] can be provided to filter the emitted events.
 */
inline fun <reified T : Event> EventEmitter<Event>.expectEvent(
        crossinline condition: (T) -> Boolean = { true }
) = runBlocking {
    suspendCoroutine { continuation ->
        lateinit var action: ((T) -> Unit)
        action = {
            if (condition(it)) {
                off(action)
                continuation.resume(Unit)
            }
        }
        on<T>(action)
    }
}

/**
 * Posts a [block] of code to the main thread and suspends until it is executed.
 */
inline fun <T> Handler.postWaiting(crossinline block: () -> T) = runBlocking {
    suspendCoroutine { continuation ->
        post { continuation.resume(block()) }
    }
}
