package com.bitmovin.analytics.conviva

import android.content.Context
import android.os.Handler
import android.util.Log
import com.bitmovin.analytics.conviva.ssai.DefaultSsaiApi
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.PlayerErrorCode
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.media.Quality
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
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
    private val ssaiApi: DefaultSsaiApi = mockk()
    private val context: Context = mockk()

    private lateinit var convivaAnalyticsIntegration: ConvivaAnalyticsIntegration

    @Before
    fun beforeTest() {
        with(ssaiApi) {
            every { isAdBreakActive } returns false
            every { reset() } just runs
        }

        convivaAnalyticsIntegration = ConvivaAnalyticsIntegration(
                player,
                "",
                context,
                ConvivaConfig(),
                videoAnalytics,
                adAnalytics,
                ssaiApi,
        )
    }

    @After
    fun afterTest() {
        clearMocks(mockedPlayer, ssaiApi, videoAnalytics, adAnalytics)
    }

    @Test
    fun `initializing subscribes to player events`() {
        expectThat(player.listeners.keys).containsExactlyInAnyOrder(attachedPlayerEvents)
    }

    @Test
    fun `releasing unsubscribes from all events`() {
        convivaAnalyticsIntegration.release()

        expectThat(player.listeners.values.flatten()).isEmpty()
    }

    @Test
    fun `reports error to ad analytics during an SSAI ad break`() {
        every { ssaiApi.isAdBreakActive } returns true

        player.listeners[PlayerEvent.Error::class]?.forEach { it(PlayerEvent.Error(PlayerErrorCode.General, "error")) }
        verify { adAnalytics.reportAdError(any(), ConvivaSdkConstants.ErrorSeverity.FATAL) }
    }

    @Test
    fun `reports player state changes to ad analytics during an SSAI ad break`() {
        every { ssaiApi.isAdBreakActive } returns true

        player.listeners[PlayerEvent.Playing::class]?.forEach { it(PlayerEvent.Playing(0.0)) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING) }

        player.listeners[PlayerEvent.Paused::class]?.forEach { it(PlayerEvent.Paused(0.0)) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED) }

        player.listeners[PlayerEvent.StallStarted::class]?.forEach { it(PlayerEvent.StallStarted()) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING) }

        player.listeners[PlayerEvent.StallEnded::class]?.forEach { it(PlayerEvent.StallEnded()) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING) }
    }

    @Test
    fun `reports video playback quality changes to ad analytics during an SSAI ad break`() {
        every { ssaiApi.isAdBreakActive } returns true
        val newVideoQuality = VideoQuality(
                id = "id",
                label = "label",
                bitrate = 2000,
                averageBitrate = 1000,
                peakBitrate = 2000,
                codec = "codec",
                frameRate = 10.3F,
                width = 400,
                height = 300,
        )
        every { mockedPlayer.playbackVideoData } returns newVideoQuality

        player.listeners[PlayerEvent.VideoPlaybackQualityChanged::class]?.forEach { onEvent ->
            onEvent(PlayerEvent.VideoPlaybackQualityChanged(null, newVideoQuality))
        }

        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, 2) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.AVG_BITRATE, 1) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, 400, 300) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, 10) }
    }

    @Test
    fun `does not report bitrate, if not available`() {
        every { ssaiApi.isAdBreakActive } returns true
        Quality.Companion.BITRATE_NO_VALUE
        val newVideoQuality = VideoQuality(
            id = "id",
            label = "label",
            bitrate = Quality.Companion.BITRATE_NO_VALUE,
            averageBitrate = Quality.Companion.BITRATE_NO_VALUE,
            peakBitrate = Quality.Companion.BITRATE_NO_VALUE,
            codec = "codec",
            frameRate = 10.3F,
            width = 400,
            height = 300,
        )
        every { mockedPlayer.playbackVideoData } returns newVideoQuality

        player.listeners[PlayerEvent.VideoPlaybackQualityChanged::class]?.forEach { onEvent ->
            onEvent(PlayerEvent.VideoPlaybackQualityChanged(null, newVideoQuality))
        }

        verify(exactly = 0) {
            adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, any())
            adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.AVG_BITRATE, any())
        }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, 400, 300) }
        verify { adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, 10) }
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

            mockkConstructor(Handler::class)
            every { anyConstructed<Handler>().postDelayed(any(), any()) } answers {
                firstArg<Runnable>().run()
                true
            }
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(Log::class)
            unmockkConstructor(Handler::class)
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
