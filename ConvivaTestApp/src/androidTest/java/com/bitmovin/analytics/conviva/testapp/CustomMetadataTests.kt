package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.api.*
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class CustomMetadataTests : TestBase() {
    @Test
    fun metadataOverrideVodExplicitInitialize() {
        val customInternTags: MutableMap<String, String> = HashMap()
        customInternTags["contentType"] = "Episode"
        val customMetadata = customMetadataOverrides(
            source = DEFAULT_DASH_VOD_SOURCE,
            streamType = ContentMetadata.StreamType.VOD,
            duration = DEFAULT_DASH_VOD_SOURCE_DURATION,
            customTags = customInternTags)

        // launch player with autoPlay enabled
        val activityScenario = setupPlayerActivityForTest(autoPlay = true, customMetadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        verifyPlayingWithMetadata(
            activityScenario = activityScenario,
            source = DEFAULT_DASH_VOD_SOURCE,
            streamType = ContentMetadata.StreamType.VOD,
            streamDuration = DEFAULT_DASH_VOD_SOURCE_DURATION,
            metadata = customMetadata,
            overrideCustom = true
        )

        activityScenario.close()
    }

    @Test
    fun metadataOverrideLiveExplicitInitalize() {
        val customInternTags: MutableMap<String, String> = HashMap()
        customInternTags["contentType"] = "Episode"
        val customMetadata = customMetadataOverrides(
            source = DEFAULT_DASH_LIVE_SOURCE,
            streamType = ContentMetadata.StreamType.LIVE,
            duration = DEFAULT_DASH_LIVE_SOURCE_DURATION,
            customTags = customInternTags)

        // launch player with autoPlay enabled
        val activityScenario = setupPlayerActivityForTest(autoPlay = true, customMetadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_LIVE_SOURCE)

        verifyPlayingWithMetadata(
            activityScenario = activityScenario,
            source = DEFAULT_DASH_LIVE_SOURCE,
            streamType = ContentMetadata.StreamType.LIVE,
            streamDuration = DEFAULT_DASH_LIVE_SOURCE_DURATION,
            metadata = customMetadata,
            overrideCustom = true
        )

        activityScenario.close()
    }

    @Test
    fun metadataOverrideCustomInternTags() {
        // Set default metadata overrides
        val customInternTags: MutableMap<String, String> = HashMap()
        customInternTags["contentType"] = "Episode"
        customInternTags["streamType"] = "CUSTOM_STREAM_TYPE"
        customInternTags["integrationVersion"] = "CUSTOM_INTEGRATION_VERSION"
        val customMetadata = customMetadataOverrides(
            source = DEFAULT_DASH_VOD_SOURCE,
            streamType = ContentMetadata.StreamType.VOD,
            duration = DEFAULT_DASH_VOD_SOURCE_DURATION,
            customTags = customInternTags)

        // launch player with autoPlay enabled
        val activityScenario = setupPlayerActivityForTest(autoPlay = true, customMetadata)

        // initialize session and verify
        initializeSession(activityScenario)

        // verify session initialization
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        verifyPlayingWithMetadata(
            activityScenario = activityScenario,
            source = DEFAULT_DASH_VOD_SOURCE,
            streamType = ContentMetadata.StreamType.VOD,
            streamDuration = DEFAULT_DASH_VOD_SOURCE_DURATION,
            metadata = customMetadata,
            overrideCustom = true
        )

        activityScenario.onActivity { activity: MainActivity ->
            verify {
                clientMock?.updateContentMetadata(
                    CONVIVA_SESSION_ID,
                    metadataEq(expectedContentMetadata(
                        source = DEFAULT_DASH_VOD_SOURCE,
                        streamType = ContentMetadata.StreamType.VOD,
                        duration = DEFAULT_DASH_VOD_SOURCE_DURATION,
                        overrideMetadata = customMetadata,
                        overrideCustom = true))
                )
            }
        }

        activityScenario.close()
    }
}
