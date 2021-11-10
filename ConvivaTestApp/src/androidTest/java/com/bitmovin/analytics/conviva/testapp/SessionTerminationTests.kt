package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.api.player.PlayerStateManager
import io.mockk.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionTerminationTests : TestBase() {

    @Test
    fun sessionEndOnPlaybackFinishedVod() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        verifyPlaying(activityScenario = activityScenario)

        // seek near to end to finish playback
        activityScenario.onActivity { activity: MainActivity ->
            // Seek close to end to finish playback
            clearMocks(playerStateManagerMock!!)
            clearMocks(clientMock!!)
            activity.bitmovinPlayer.seek(activity.bitmovinPlayer.duration - 1)
            Thread.sleep(4000)
        }

        // verify session termination
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
            }
            verifyOrder {
                clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
            }
        }
    }

    @Test
    fun sessionEndOnSourceUnloadedVod() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        verifyPlaying(activityScenario = activityScenario)

        // Unload source
        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                clearMocks(clientMock!!)
                activity.bitmovinPlayer.unload()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        // verify session termination
        activityScenario.onActivity { activity: MainActivity ->
            verify(inverse = true) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
            }
            verifyOrder {
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
            }
        }
    }

    @Test
    fun explicitSessionEndVod() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        verifyPlaying(activityScenario = activityScenario)

        // end session explicitly
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(playerStateManagerMock!!)
            clearMocks(clientMock!!)
            convivaAnalytics?.endSession()
            Thread.sleep(2000)
        }

        // verify session termination
        activityScenario.onActivity { activity: MainActivity ->
            verify(inverse = true) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
            }
            verifyOrder {
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
            }
        }
    }

    @Test
    fun sessionEndOnSourceUnloadedLive() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_LIVE_SOURCE)

        verifyPlaying(activityScenario = activityScenario)

        // Unload source
        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                clearMocks(clientMock!!)
                activity.bitmovinPlayer.unload()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        // verify session termination
        activityScenario.onActivity { activity: MainActivity ->
            verify(inverse = true) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
            }
            verifyOrder {
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
            }
        }
    }

    @Test
    fun explicitSessionEndLive() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_LIVE_SOURCE)

        verifyPlaying(activityScenario = activityScenario)

        // end session explicitly
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(playerStateManagerMock!!)
            clearMocks(clientMock!!)
            convivaAnalytics?.endSession()
            Thread.sleep(2000)
        }

        // verify session termination
        activityScenario.onActivity { activity: MainActivity ->
            verify(inverse = true) {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
            }
            verifyOrder {
                clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                clientMock?.releasePlayerStateManager(playerStateManagerMock)
            }
        }
    }
}
