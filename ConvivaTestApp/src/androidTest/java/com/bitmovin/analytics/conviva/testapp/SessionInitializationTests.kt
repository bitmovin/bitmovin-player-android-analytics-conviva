package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionInitializationTests : TestBase() {
    // If you experience failing tests in the emulator, try restricting network on your emulate to ensure BUFFERING is triggered.
    @Test
    fun implicitSessionStartOnManualPlayVOD() {
        val metadata = customMetadataOverrides()

        // launch player without autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = false, metadata)

        // load and play source
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_VOD_SOURCE)
        playSource(activityScenario = activityScenario)

        // verify session initialization and playing state
        verifySessionInitialization(activityScenario, metadata, true)
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun implicitSessionStartAutoPlayVOD() {
        val metadata = customMetadataOverrides()

        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // load and play source
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_VOD_SOURCE)
        playSource(activityScenario = activityScenario)

        // verify session initialization and playing state
        verifySessionInitialization(activityScenario, metadata, true)
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun explicitSessionStartManualPlayVOD() {
        val metadata = customMetadataOverrides()

        // launch player without autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = false, metadata)

        // initialize and verify session
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario, metadata)

        // load + play source and verify playing state
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_VOD_SOURCE)
        playSource(activityScenario = activityScenario)
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun explicitSessionStartAutoPlayVOD() {
        val metadata = customMetadataOverrides()

        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize and verify session
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario,metadata)

        // load source and playback should auto start
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_VOD_SOURCE)
        // verify playing tracking
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun implicitSessionStartOnManualPlayLive() {
        val metadata = customMetadataOverrides()

        // launch player without autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = false, metadata)

        // load and play source
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_LIVE_SOURCE)
        playSource(activityScenario = activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario, metadata, true)

        // verify playing tracking
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun implicitSessionStartAutoPlayLive() {
        val metadata = customMetadataOverrides()

        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // load and play source
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_LIVE_SOURCE)
        playSource(activityScenario = activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario, metadata, true)

        // verify playing tracking
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun explicitSessionStartManualPlayLive() {
        val metadata = customMetadataOverrides()

        // launch player without autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = false, metadata)

        // initialize session
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario, metadata)

        // load and play source
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_LIVE_SOURCE)
        playSource(activityScenario = activityScenario)

        // verify playing tracking
        verifyPlaying(activityScenario = activityScenario)
    }

    @Test
    fun explicitSessionStartAutoPlayLive() {
        val metadata = customMetadataOverrides()

        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata)

        // initialize session
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionInitialization(activityScenario, metadata)

        // load source and playback should auto start
        loadSource(activityScenario = activityScenario, source = DEFAULT_DASH_LIVE_SOURCE)
        // verify playing tracking
        verifyPlaying(activityScenario = activityScenario)
    }
}
