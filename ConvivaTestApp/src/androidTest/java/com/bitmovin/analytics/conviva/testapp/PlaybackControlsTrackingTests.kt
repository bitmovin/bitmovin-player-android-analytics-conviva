package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bitmovin.player.api.event.PlayerEvent.Muted
import com.bitmovin.player.api.event.PlayerEvent.Unmuted
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.conviva.api.player.PlayerStateManager
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackContorlsTrackingTests: TestBase() {
    @Test
    fun pauseResume() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        Thread.sleep(2000)
        verifyPlaying(activityScenario = activityScenario)

        // pause playback
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(playerStateManagerMock!!)
            activity.bitmovinPlayer.pause()
        }
        Thread.sleep(2000)
        // verify pause tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify(atLeast=1) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PAUSED)
            }
        }

        // resume playback
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(playerStateManagerMock!!)
            activity.bitmovinPlayer.play()
        }
        Thread.sleep(2000)
        // verify resume tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify(exactly=1) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
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
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        Thread.sleep(2000)
        verifyPlaying(activityScenario = activityScenario)

        // seek
        activityScenario.onActivity { activity: MainActivity ->
            // Seek playback
            clearMocks(playerStateManagerMock!!)
            activity.bitmovinPlayer.seek(120.0)
        }
        Thread.sleep(2000)
        // verify seek tracking
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder {
                playerStateManagerMock?.setPlayerSeekStart(120000)
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                playerStateManagerMock?.setPlayerSeekEnd()
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
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
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_LIVE_SOURCE)
        Thread.sleep(2000)
        verifyPlaying(activityScenario = activityScenario)

        // timeshift
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(playerStateManagerMock!!)
            activity.bitmovinPlayer.timeShift(30.0)
        }
        Thread.sleep(2000)
        // verify timeshift tracking
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder {
                playerStateManagerMock?.setPlayerSeekStart(-1)
                playerStateManagerMock?.setPlayerSeekEnd()
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
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        Thread.sleep(2000)
        verifyPlaying(activityScenario = activityScenario)

        // mute playback
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(clientMock!!)
            activity.bitmovinPlayer.mute()
        }
        Thread.sleep(1000)
        // verify mute tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                val eventName: String? = Muted::class.simpleName
                clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, "on$eventName", any())
            }
        }

        // unmute playback
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(clientMock!!)
            activity.bitmovinPlayer.unmute()
        }
        Thread.sleep(1000)

        // verify unmute tracking
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                val eventName: String? = Unmuted::class.simpleName
                clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, "on$eventName", any())
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
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        Thread.sleep(2000)
        verifyPlaying(activityScenario = activityScenario)

        // switch video quality
        var switchToVideoQuality: VideoQuality? = null
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(playerStateManagerMock!!)
            clearMocks(clientMock!!)
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
                playerStateManagerMock?.setBitrateKbps(switchToVideoQuality!!.bitrate / 1000)
                playerStateManagerMock?.setVideoHeight(switchToVideoQuality!!.height)
                playerStateManagerMock?.setVideoWidth(switchToVideoQuality!!.width)
            }
            verify {
                clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
            }
        }
    }
}
