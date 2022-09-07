package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bitmovin.player.api.event.PlayerEvent.Muted
import com.bitmovin.player.api.event.PlayerEvent.Unmuted
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackControlsTrackingTests: TestBase() {
    @Test
    fun pauseResume() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        // pause playback
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            activity.bitmovinPlayer.pause()
        }
        Thread.sleep(2000)
        // verify pause tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify(atLeast=1) {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PAUSED)
            }
        }

        // resume playback
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            activity.bitmovinPlayer.play()
        }
        Thread.sleep(2000)
        // verify resume tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify(exactly=1) {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
            }
        }
    }

    @Test
    fun vodSeek() {
        // launch player with autoPlay enabled and initialize session
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        // seek
        activityScenario.onActivity { activity: MainActivity ->
            // Seek playback
            clearMocks(videoAnalyticsMock!!)
            activity.bitmovinPlayer.seek(120.0)
        }
        Thread.sleep(2000)
        // verify seek tracking
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED,120000)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED)
            }
        }
    }

    @Test
    fun liveTimeshift() {
        // launch player with autoPlay enabled and initialize session
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_LIVE_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        // timeshift
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            activity.bitmovinPlayer.timeShift(30.0)
        }
        Thread.sleep(2000)
        // verify timeshift tracking
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED,-1)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED)
            }
        }
    }

    @Test
    fun muteUnmute() {
        // launch player with autoPlay enabled and initialize session
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        // mute playback
        activityScenario.onActivity { activity: MainActivity ->
            activity.bitmovinPlayer.mute()
        }
        Thread.sleep(1000)
        // verify mute tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                val eventName: String? = Muted::class.simpleName
                ConvivaAnalytics.reportAppEvent("on$eventName", HashMap())
            }
        }

        // unmute playback
        activityScenario.onActivity { activity: MainActivity ->
            activity.bitmovinPlayer.unmute()
        }
        Thread.sleep(1000)

        // verify unmute tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                val eventName: String? = Unmuted::class.simpleName
                ConvivaAnalytics.reportAppEvent("on$eventName", HashMap())
            }
        }
    }

    @Test
    fun qualitySwitch() {
        // launch player with autoPlay enabled and initialize session
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        // switch video quality
        var switchToVideoQuality: VideoQuality? = null
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            val videoQualityArr = activity.bitmovinPlayer.availableVideoQualities
            val currVideoQuality = activity.bitmovinPlayer.videoQuality
            switchToVideoQuality = videoQualityArr[0]
            for (quality in videoQualityArr) {
                if (quality.bitrate < switchToVideoQuality!!.bitrate && quality.id !== currVideoQuality!!.id) {
                    switchToVideoQuality = quality
                }
            }
            activity.bitmovinPlayer.setVideoQuality(switchToVideoQuality?.id!!)
        }
        Thread.sleep(2000)

        // verify quality tracking
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder() {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, switchToVideoQuality!!.height, switchToVideoQuality!!.width)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, switchToVideoQuality!!.bitrate / 1000)

            }
            verify {
                videoAnalyticsMock?.setContentInfo(any())
            }
        }
    }
}
