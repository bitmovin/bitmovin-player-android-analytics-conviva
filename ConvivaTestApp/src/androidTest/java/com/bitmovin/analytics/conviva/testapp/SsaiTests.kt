package com.bitmovin.analytics.conviva.testapp

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration
import com.bitmovin.analytics.conviva.ConvivaConfig
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.bitmovin.analytics.conviva.ssai.SsaiApi
import com.bitmovin.analytics.conviva.testapp.framework.BITMOVIN_PLAYER_LICENSE_KEY
import com.bitmovin.analytics.conviva.testapp.framework.CONVIVA_CUSTOMER_KEY
import com.bitmovin.analytics.conviva.testapp.framework.CONVIVA_GATEWAY_URL
import com.bitmovin.analytics.conviva.testapp.framework.Sources
import com.bitmovin.analytics.conviva.testapp.framework.expectEvent
import com.bitmovin.analytics.conviva.testapp.framework.postWaiting
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith


/**
 * This test class does not verify any specific behavior, but rather can be used to validate the
 * integration against the [Conviva Touchstone integration test tool](https://touchstone.conviva.com/).
 */
@RunWith(AndroidJUnit4::class)
class SsaiTests {
    /**
     * Plays a vod stream and fakes a SSAI ad break with a single 5 seconds ad and a  1 seconds slate.
     */
    @Test
    fun reports_ad_analytics_for_mid_roll_ad() {
        val adStart = 5.0
        val adDuration = 5.0
        val slateStart = adStart + adDuration
        val slateDuration = 1.0


        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mainHandler = Handler(context.mainLooper)
        val player = mainHandler.postWaiting {
            Player(
                    context,
                    PlayerConfig(
                            key = BITMOVIN_PLAYER_LICENSE_KEY,
                            playbackConfig = PlaybackConfig(
                                    isAutoplayEnabled = true,
                            ),
                    ),
                    analyticsConfig = AnalyticsPlayerConfig.Disabled,
            )
        }

        val integration = mainHandler.postWaiting {
            val convivaAnalyticsIntegration = ConvivaAnalyticsIntegration(
                    player,
                    CONVIVA_CUSTOMER_KEY,
                    context,
                    ConvivaConfig().apply {
                        isDebugLoggingEnabled = true
                        gatewayUrl = CONVIVA_GATEWAY_URL
                    },
            )

            convivaAnalyticsIntegration.updateContentMetadata(
                    MetadataOverrides()
                            .apply {
                                applicationName = "Bitmovin Android Conviva integration example app"
                                viewerId = "testViewerId"
                            }
            )
            convivaAnalyticsIntegration
        }

        mainHandler.postWaiting { player.load(Sources.Dash.basic) }
        player.expectEvent<PlayerEvent.TimeChanged> { it.time > adStart } // play main content until ad start

        // fake ad break
        mainHandler.postWaiting {
            integration.ssai.reportAdBreakStarted()
            integration.ssai.reportAdStarted(SsaiApi.AdInfo().apply {
                duration = adDuration
                isSlate = false
                id = "testAdId"
                title = "testAdTitle"
            })
        }

        player.expectEvent<PlayerEvent.TimeChanged> { it.time > adStart + adDuration } // wait unitl ad is over


        mainHandler.postWaiting {
            integration.ssai.reportAdFinished()
            integration.ssai.reportAdStarted(
                    SsaiApi.AdInfo().apply {
                        duration = slateDuration
                        isSlate = true
                        title = "testSlate"
                    }
            )
        }

        player.expectEvent<PlayerEvent.TimeChanged> { it.time > slateStart + slateDuration } // wait for five more seconds of playback

        mainHandler.postWaiting {
            integration.ssai.reportAdFinished()
            integration.ssai.reportAdBreakFinished()
        }

        mainHandler.postWaiting { player.destroy() }
        runBlocking { delay(1000) }
    }
}
