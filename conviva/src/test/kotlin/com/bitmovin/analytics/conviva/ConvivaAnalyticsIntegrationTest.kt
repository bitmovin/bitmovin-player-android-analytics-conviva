package com.bitmovin.analytics.conviva

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.conviva.fixtures.MockPlayer
import com.bitmovin.analytics.conviva.helper.mockLogging
import com.bitmovin.analytics.conviva.helper.unmockLogging
import com.bitmovin.analytics.conviva.ssai.DefaultSsaiApi
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.advertising.Ad
import com.bitmovin.player.api.advertising.AdBreak
import com.bitmovin.player.api.advertising.AdData
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.deficiency.PlayerErrorCode
import com.bitmovin.player.api.deficiency.PlayerWarningCode
import com.bitmovin.player.api.deficiency.SourceWarningCode
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.media.Quality
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaSdkConstants.AdPosition
import com.conviva.sdk.ConvivaVideoAnalytics
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty

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
            every { setPlayer(any()) } just runs
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
    fun `updating after releasing does nothing`() {
        convivaAnalyticsIntegration.release()

        expectThat(convivaAnalyticsIntegration.sessionActive == false)

        convivaAnalyticsIntegration.updateContentMetadata(MetadataOverrides())
        verify(exactly = 0) { videoAnalytics.setContentInfo(any()) }
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

    @Test
    fun `does not report a playback error when receiving a player warning event without active conviva session`() {
        player.listeners[PlayerEvent.Warning::class]?.forEach { onEvent ->
            onEvent(PlayerEvent.Warning(PlayerWarningCode.General, "warning"))
        }
        verify(exactly = 0) { videoAnalytics.reportPlaybackError(any(), any()) }
    }

    @Test
    fun `does not report a playback error when receiving a source warning event without active conviva session`() {
        player.listeners[SourceEvent.Warning::class]?.forEach { onEvent ->
            onEvent(SourceEvent.Warning(SourceWarningCode.General, "warning"))
        }
        verify(exactly = 0) { videoAnalytics.reportPlaybackError(any(), any()) }
    }

    @Test
    fun `reports CSAI ad position based on last ad break schedule time`() {
        player.listeners[PlayerEvent.AdBreakStarted::class]?.forEach {
            it(createAdBreakStartedEvent(10.0))
        }

        verify { videoAnalytics.reportAdBreakStarted(any(), any()) }
        player.listeners[PlayerEvent.AdStarted::class]?.forEach { it(TEST_AD_STARTED_EVENT) }
        verify {
            adAnalytics.reportAdStarted(match { it["c3.ad.position"] == AdPosition.MIDROLL })
        }

        player.listeners[PlayerEvent.AdBreakStarted::class]?.forEach {
            it(createAdBreakStartedEvent(00.0))
        }

        verify { videoAnalytics.reportAdBreakStarted(any(), any()) }
        player.listeners[PlayerEvent.AdStarted::class]?.forEach { it(TEST_AD_STARTED_EVENT) }
        verify {
            adAnalytics.reportAdStarted(match { it["c3.ad.position"] == AdPosition.PREROLL })
        }

    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockLogging()

            mockkConstructor(Handler::class)
            every { anyConstructed<Handler>().postDelayed(any(), any()) } answers {
                firstArg<Runnable>().run()
                true
            }
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockLogging()
            unmockkConstructor(Handler::class)
        }
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

private val TEST_AD_STARTED_EVENT = PlayerEvent.AdStarted(
        clientType = AdSourceType.Ima,
        clickThroughUrl = "clickThroughUrl",
        duration = 10.0,
        timeOffset = 10.0,
        position = "0.0",
        skipOffset = 10.0,
        ad = mockk(relaxed = true),
        indexInQueue = 0,
)

private fun createAdBreakStartedEvent(scheduleTime: Double): PlayerEvent.AdBreakStarted {
    val adBreakStarted = PlayerEvent.AdBreakStarted(
        adBreak = mockk {
            every { this@mockk.scheduleTime } returns scheduleTime
        }
    )
    return adBreakStarted
}
