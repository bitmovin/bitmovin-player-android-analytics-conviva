package com.bitmovin.analytics.conviva

import android.content.Context
import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaVideoAnalytics
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import kotlin.reflect.KClass

class ConvivaAnalyticsIntegrationTest {
    private val mockedPlayer: Player = mockk(relaxed = true)
    private val player: MockPlayer = MockPlayer(mockedPlayer)
    private val videoAnalytics: ConvivaVideoAnalytics = mockk(relaxed = true)
    private val adAnalytics: ConvivaAdAnalytics = mockk(relaxed = true)
    private val context: Context = mockk()

    private lateinit var convivaAnalyticsIntegration: ConvivaAnalyticsIntegration

    @After
    fun afterTest() {
        clearMocks(mockedPlayer)
    }

    @Test
    fun `initializing subscribes to player events`() {
        convivaAnalyticsIntegration = ConvivaAnalyticsIntegration(
                player,
                "",
                context,
                ConvivaConfig(),
                videoAnalytics,
                adAnalytics
        )

        expectThat(player.listeners.keys).containsExactlyInAnyOrder(attachedPlayerEvents)
    }

    @Test
    fun `releasing unsubscribes from all events`() {
        convivaAnalyticsIntegration = ConvivaAnalyticsIntegration(
                player,
                "",
                context,
                ConvivaConfig(),
                videoAnalytics,
                adAnalytics
        )

        convivaAnalyticsIntegration.release()

        expectThat(player.listeners.values.flatten()).isEmpty()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockkStatic(Log::class)
            every { Log.v(any(), any()) } returns 0
            every { Log.d(any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
        }
    }
}

/**
 * A test implementation of the [Player] interface that allows overriding its behavior and falling
 * back to a mocked player for not overridden methods for convenience.
 */
@Suppress("UNCHECKED_CAST")
private class MockPlayer(private val player: Player) : Player by player {

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

private val attachedPlayerEvents = listOf(
        PlayerEvent.Play::class,
        PlayerEvent.Paused::class,
        PlayerEvent.Error::class,
        PlayerEvent.Warning::class,
        PlayerEvent.Muted::class,
        PlayerEvent.Unmuted::class,
        PlayerEvent.Playing::class,
        PlayerEvent.StallEnded::class,
        PlayerEvent.StallStarted::class,
        PlayerEvent.PlaybackFinished::class,
        PlayerEvent.Seek::class,
        PlayerEvent.Seeked::class,
        PlayerEvent.TimeShift::class,
        PlayerEvent.TimeShifted::class,
        PlayerEvent.AdBreakFinished::class,
        PlayerEvent.AdBreakStarted::class,
        PlayerEvent.AdStarted::class,
        PlayerEvent.AdFinished::class,
        PlayerEvent.AdSkipped::class,
        PlayerEvent.AdError::class,
        PlayerEvent.TimeChanged::class,
        PlayerEvent.VideoPlaybackQualityChanged::class,
        SourceEvent.Unloaded::class,
        SourceEvent.Error::class,
        SourceEvent.Warning::class,
)
