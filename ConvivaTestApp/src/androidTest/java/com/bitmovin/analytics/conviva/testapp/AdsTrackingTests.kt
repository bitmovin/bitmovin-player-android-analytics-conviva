package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bitmovin.analytics.conviva.testapp.framework.Sources
import com.conviva.api.*
import com.conviva.sdk.ConvivaSdkConstants
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdsTrackingTests : TestBase() {
    @Test
    fun test_adEventsVmapPrerollSingleAd() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata, Sources.Ads.VMAP_PREROLL_SINGLE_TAG)

        // initialize session and verify
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        Thread.sleep(8000) // Bit flakey with smaller timeouts than this

        // verify Ad start
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                videoAnalyticsMock?.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.CLIENT_SIDE)
            }
        }
    }
}
