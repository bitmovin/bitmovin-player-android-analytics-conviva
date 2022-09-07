package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.api.*
import com.conviva.sdk.ConvivaSdkConstants
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackErrorTrackingTests: TestBase() {
    @Test
    fun errorBeforeSessionStart() {
        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, defaultMetadataOverrides())

        // load invalid asset without initializing Conviva session explicitly
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            activity.bitmovinPlayer.load(INVALID_DASH_VOD_SOURCE)
        }
        Thread.sleep(2000)

        // verify error tracking
        activityScenario.onActivity { activity: MainActivity ->
            // verify session is started before sending error event
            verifyOrder {
                    convivaAnalyticsIntegration?.updateContentMetadata(any())
                }
            verifyOrder {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.STOPPED)
            }
            // verify playback is not reported as buffering and playing
            verifyAll(inverse=true) {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
            }
//            // verify error is reported to Conviva and session is ended
            verifyOrder {
                // Conviva error is reported
                videoAnalyticsMock?.reportPlaybackError(any(), ConvivaSdkConstants.ErrorSeverity.FATAL)
                videoAnalyticsMock?.reportPlaybackEnded()
            }
        }
    }

    @Test
    fun errorAfterSessionStart() {
        // launch player with autoPlay enabled without initializing session
        activityScenario = setupPlayerActivityForTest(autoPlay = true, defaultMetadataOverrides())

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario)

        // load invalid DASH source
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            activity.bitmovinPlayer.load(INVALID_DASH_VOD_SOURCE)
        }
        Thread.sleep(2000)
        // verify
        activityScenario.onActivity { activity: MainActivity ->
            // verify session is not started again
            verifyAll(inverse=true) {
                convivaAnalyticsIntegration?.updateContentMetadata(any())
            }

            verifyAll(inverse=true) {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING)
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING)
            }

//            // verify error is reported to Conviva and session is ended
            verifyOrder {
                // Conviva error is reported
                videoAnalyticsMock?.reportPlaybackError(any(), ConvivaSdkConstants.ErrorSeverity.FATAL)
                videoAnalyticsMock?.reportPlaybackEnded()
            }
        }
    }
}
