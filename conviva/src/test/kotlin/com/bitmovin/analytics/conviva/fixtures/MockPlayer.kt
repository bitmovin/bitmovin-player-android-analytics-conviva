package com.bitmovin.analytics.conviva.fixtures

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import kotlin.reflect.KClass

/**
 * A test implementation of the [Player] interface that allows overriding its behavior and falling
 * back to a mocked player for not overridden methods for convenience.
 */
@Suppress("UNCHECKED_CAST")
class MockPlayer(private val player: Player) : Player by player {

    val listeners = mutableMapOf<KClass<out Event>, List<(Event) -> Unit>>()
    override fun <E : Event> on(eventClass: KClass<E>, action: (E) -> Unit) {
        listeners[eventClass] = listeners[eventClass].orEmpty() + action as (Event) -> Unit
    }

    override fun <E : Event> on(eventClass: Class<E>, eventListener: EventListener<in E>) {
        listeners[eventClass.kotlin] = listeners[eventClass.kotlin].orEmpty() + eventListener::onEvent as (Event) -> Unit
    }

    override fun <E : Event> off(action: (E) -> Unit) {
        listeners.entries.removeIf { it.value == action as (Event) -> Unit }
    }

    override fun <E : Event> off(eventClass: KClass<E>, action: (E) -> Unit) {
        listeners[eventClass] = listeners[eventClass].orEmpty() - action as (Event) -> Unit
    }

    override fun <E : Event> off(eventListener: EventListener<in E>) {
        listeners.entries.removeIf { it.value == eventListener::onEvent as (Event) -> Unit }
    }

    override fun <E : Event> off(eventClass: Class<E>, eventListener: EventListener<in E>) {
        listeners[eventClass.kotlin] = listeners[eventClass.kotlin].orEmpty() - eventListener::onEvent as (Event) -> Unit
    }

}