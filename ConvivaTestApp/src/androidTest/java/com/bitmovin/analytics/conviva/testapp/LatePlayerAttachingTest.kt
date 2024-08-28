package com.bitmovin.analytics.conviva.testapp

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration
import com.bitmovin.analytics.conviva.ConvivaConfig
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.bitmovin.analytics.conviva.testapp.framework.BITMOVIN_PLAYER_LICENSE_KEY
import com.bitmovin.analytics.conviva.testapp.framework.CONVIVA_CUSTOMER_KEY
import com.bitmovin.analytics.conviva.testapp.framework.CONVIVA_GATEWAY_URL
import com.bitmovin.analytics.conviva.testapp.framework.Sources
import com.bitmovin.analytics.conviva.testapp.framework.expectEvent
import com.bitmovin.analytics.conviva.testapp.framework.postWaiting
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
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
class LatePlayerAttachingTest {
    /**
     * Creates a tracking session and attaches the Player instance after the session has been initialized.
     * The VST should be around 3 second in Touchstone.
     */
    @Test
    fun includesTimeBetweenSessionInitializationAndSourceLoadingAsVST() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mainHandler = Handler(context.mainLooper)

        val convivaAnalyticsIntegration = mainHandler.postWaiting {
            val convivaAnalyticsIntegration = ConvivaAnalyticsIntegration(
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
                        assetName = "Asset Name"
                    }
            )
            convivaAnalyticsIntegration.initializeSession()
            convivaAnalyticsIntegration
        }

        runBlocking { delay(2000) }

        val player = mainHandler.postWaiting {
            val player = Player(
                context,
                PlayerConfig(
                    key = BITMOVIN_PLAYER_LICENSE_KEY,
                    playbackConfig = PlaybackConfig(
                        isAutoplayEnabled = true,
                    ),
                ),
                analyticsConfig = AnalyticsPlayerConfig.Disabled,
            )
            player
        }

        mainHandler.postWaiting { convivaAnalyticsIntegration.attachPlayer(player) }

        runBlocking { delay(1000) }

        mainHandler.postWaiting { player.load(Sources.Dash.basic) }

        player.expectEvent<PlayerEvent.TimeChanged> { it.time > 5.0 }

        mainHandler.postWaiting { player.destroy() }
        runBlocking { delay(1000) }
    }
}
