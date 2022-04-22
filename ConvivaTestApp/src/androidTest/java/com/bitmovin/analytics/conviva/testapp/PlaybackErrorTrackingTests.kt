package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.api.*
import com.conviva.api.player.PlayerStateManager
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
            activity.bitmovinPlayer.load(INVALID_DASH_VOD_SOURCE)
        }
        Thread.sleep(2000)

        // verify error tracking
        activityScenario.onActivity { activity: MainActivity ->
            // verify session is started before sending error event
            verifyOrder {
                clientMock?.createSession(any())
                clientMock?.playerStateManager
                clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                clientMock?.attachPlayer(CONVIVA_SESSION_ID, any())
            }
            verifyOrder {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                playerStateManagerMock?.setClientMeasureInterface(any())
            }
            // verify playback is not reported as buffering and playing
            verifyAll(inverse=true) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
            }
            // verify error is reported to Conviva and session is ended
            verifyOrder {
                // Conviva error is reported
                clientMock?.reportError(CONVIVA_SESSION_ID, any(), ConvivaSdkConstants.ErrorSeverity.FATAL)

                // Conviva session is ended
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
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
        verifySessionIntialization(activityScenario)

        // load invalid DASH source
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(clientMock!!)
            clearMocks(playerStateManagerMock!!)
            activity.bitmovinPlayer.load(INVALID_DASH_VOD_SOURCE)
        }
        Thread.sleep(2000)
        // verify
        activityScenario.onActivity { activity: MainActivity ->
            // verify session is not started again
            verifyAll(inverse=true) {
                clientMock?.createSession(any())
                clientMock?.attachPlayer(CONVIVA_SESSION_ID, any())
            }
            verifyAll(inverse=true) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
            }

            // verify error is reported to Conviva and session is ended
            verifyOrder {
                // Conviva error is reported
                clientMock?.reportError(CONVIVA_SESSION_ID, any(), ConvivaSdkConstants.ErrorSeverity.FATAL)

                // Conviva session is ended
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
            }
        }
    }
}
