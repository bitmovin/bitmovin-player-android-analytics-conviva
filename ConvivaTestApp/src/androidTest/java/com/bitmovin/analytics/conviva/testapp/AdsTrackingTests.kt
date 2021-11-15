package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.api.*
import io.mockk.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdsTrackingTests : TestBase() {
    val VMAP_PREROLL_SINGLE_TAG = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator="

    @Test
    fun test_adEventsVmapPrerollSingleAd() {
        // launch player with autoPlay enabled
        val metadata = defaultMetadataOverrides()
        activityScenario = setupPlayerActivityForTest(autoPlay = true, metadata, VMAP_PREROLL_SINGLE_TAG)

        // initialize session and verify
        initializeSession(activityScenario)
        verifySessionIntialization(activityScenario)

        // load source and verify
        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)
        verifyPlaying(activityScenario = activityScenario)

        // verify Ad start
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder {
                clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                clientMock?.adStart(
                    CONVIVA_SESSION_ID,
                    Client.AdStream.SEPARATE,
                    Client.AdPlayer.CONTENT,
                    Client.AdPosition.PREROLL
                )
            }
        }
    }
}
