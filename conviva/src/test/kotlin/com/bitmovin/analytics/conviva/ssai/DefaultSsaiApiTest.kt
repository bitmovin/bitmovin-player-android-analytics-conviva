package com.bitmovin.analytics.conviva.ssai

import android.util.Log
import com.conviva.sdk.ConvivaAdAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue


class DefaultSsaiApiTest {
    private val videoAnalytics: ConvivaVideoAnalytics = mockk(relaxed = true)
    private val adAnalytics: ConvivaAdAnalytics = mockk()
    private val playbackInfoProvider = mockk<PlaybackInfoProvider>()
    private lateinit var ssaiApi: DefaultSsaiApi

    @Before
    fun beforeTest() {
        every { playbackInfoProvider.playerState } returns ConvivaSdkConstants.PlayerState.PLAYING
        every { playbackInfoProvider.playbackVideoData } returns hashMapOf<String, Array<Any>>(
                ConvivaSdkConstants.PLAYBACK.BITRATE to arrayOf(1),
                ConvivaSdkConstants.PLAYBACK.RESOLUTION to arrayOf(800, 1600),
                ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE to arrayOf(60),
        )
        with(adAnalytics) {
            every { reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, any()) } just runs
            every { reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, any(), any()) } just runs
            every { reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, any()) } just runs
            every { reportAdMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, any()) } just runs
            every { reportAdStarted(any()) } just runs
            every { reportAdEnded() } just runs
            every { reportAdSkipped() } just runs
            every { setAdInfo(any()) } just runs
        }

        ssaiApi = DefaultSsaiApi(
                videoAnalytics,
                adAnalytics,
                playbackInfoProvider,
        )
    }

    @After
    fun afterTest() {
        clearMocks(videoAnalytics, adAnalytics, playbackInfoProvider)
    }


    @Test
    fun `reports server side ad break start to conviva after  ad break started`() {
        ssaiApi.reportAdBreakStarted()
        verify {
            videoAnalytics.reportAdBreakStarted(
                    ConvivaSdkConstants.AdPlayer.CONTENT,
                    ConvivaSdkConstants.AdType.SERVER_SIDE,
                    any(),
            )
        }
    }

    @Test
    fun `has active ad break after ad break started`() {
        ssaiApi.reportAdBreakStarted()
        expectThat(ssaiApi.isAdBreakActive).isTrue()
    }

    @Test
    fun `has no active ad break after ad break finished`() {
        ssaiApi.reportAdBreakStarted()
        expectThat(ssaiApi.isAdBreakActive).isTrue()

        ssaiApi.reportAdBreakFinished()
        expectThat(ssaiApi.isAdBreakActive).isFalse()
    }

    @Test
    fun `reports ad start with default values to conviva when ad starts`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())

        val slot = slot<MutableMap<String, Any>>()
        verify {
            adAnalytics.reportAdStarted(capture(slot))
        }

        expectThat(slot.captured.toList())
                .containsExactlyInAnyOrder(
                        listOf(
                                "c3.ad.id" to "NA",
                                "c3.ad.system" to "NA",
                                "c3.ad.mediaFileApiFramework" to "NA",
                                "c3.ad.firstAdSystem" to "NA",
                                "c3.ad.firstAdId" to "NA",
                                "c3.ad.firstCreativeId" to "NA",
                                "c3.ad.technology" to "Server Side",
                                "c3.ad.isSlate" to false,
                        )
                )
    }

    @Test
    fun `reports ad playback state playing to conviva when ad starts`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())

        verify {
            adAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PLAYING,
            )
        }
    }

    @Test
    fun `reports ad playback state playing to conviva when ad starts while paused`() {
        every { playbackInfoProvider.playerState } returns ConvivaSdkConstants.PlayerState.PAUSED

        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())

        verify {
            adAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.PAUSED,
            )
        }
    }

    @Test
    fun `reports ad playback state buffering to conviva when ad starts while stalling`() {
        every { playbackInfoProvider.playerState } returns
                ConvivaSdkConstants.PlayerState.BUFFERING

        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())

        verify {
            adAnalytics.reportAdMetric(
                    ConvivaSdkConstants.PLAYBACK.PLAYER_STATE,
                    ConvivaSdkConstants.PlayerState.BUFFERING,
            )
        }
    }

    @Test
    fun `reports current playback video quality to conviva on ad start`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())

        verify {
            adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, 1)
            adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, 800, 1600)
            adAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, 60)
        }
    }

    @Test
    fun `reports ad start with overwritten metadata when ad starts with additional metadata`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo().apply {
            id = "adIdSetInFields"
            additionalMetadata = mapOf("c3.ad.id" to "adIdSetInAdditionalMetadata")
        })

        val slot = slot<MutableMap<String, Any>>()
        verify {
            adAnalytics.reportAdStarted(capture(slot))
        }

        expectThat(slot.captured["c3.ad.id"]).isEqualTo("adIdSetInAdditionalMetadata")
    }

    @Test
    fun `reports ad end to conviva when ad finished`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())
        ssaiApi.reportAdFinished()

        verify { adAnalytics.reportAdEnded() }
    }

    @Test
    fun `reports ad break end to conviva when ad break finished`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdBreakFinished()

        verify { videoAnalytics.reportAdBreakEnded() }
    }

    @Test
    fun `reports ad skipped to conviva when ad skipped`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())
        ssaiApi.reportAdSkipped()

        verify { adAnalytics.reportAdSkipped() }
        expectThat(ssaiApi.isAdBreakActive).isTrue()
    }

    @Test
    fun `reports ad metadata to conviva when updating ad info`() {
        ssaiApi.reportAdBreakStarted()

        val testAdInfo = SsaiApi.AdInfo().apply {
            id = "adId"
            adSystem = "adSystem"
            title = "title"
            position = AdPosition.PREROLL
            adStitcher = "adStitcher"
            duration = 1000.0
            isSlate = true
            additionalMetadata = mapOf("key" to "value")
        }

        ssaiApi.updateAdInfo(testAdInfo)

        val slot = slot<MutableMap<String, Any>>()
        verify { adAnalytics.setAdInfo(capture(slot)) }

        expectThat(slot.captured.toList())
                .contains(
                        listOf(
                                "c3.ad.id" to "adId",
                                "c3.ad.system" to "adSystem",
                                ConvivaSdkConstants.ASSET_NAME to "title",
                                "c3.ad.position" to ConvivaSdkConstants.AdPosition.PREROLL,
                                "c3.ad.stitcher" to "adStitcher",
                                ConvivaSdkConstants.DURATION to 1000.0,
                                "c3.ad.isSlate" to true,
                                "key" to "value",
                        )
                )
    }

    @Test
    fun `ends potential ads and ad breaks when resetting`() {
        ssaiApi.reportAdBreakStarted()
        ssaiApi.reportAdStarted(SsaiApi.AdInfo())

        ssaiApi.reset()

        verify { adAnalytics.reportAdEnded() }
        verify { videoAnalytics.reportAdBreakEnded() }
        expectThat(ssaiApi.isAdBreakActive).isFalse()
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

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unmockkStatic(Log::class)
        }
    }
}
