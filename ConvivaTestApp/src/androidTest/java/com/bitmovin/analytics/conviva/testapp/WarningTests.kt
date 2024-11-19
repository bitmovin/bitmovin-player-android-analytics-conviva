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
import com.bitmovin.analytics.conviva.testapp.framework.callAndExpectEvent
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
class WarningTests {
    /**
     * Triggers a warning outside of the active conviva session. No integration error must be
     * shown in Touchstone.
     */
    @Test
    fun warning_outside_of_active_conviva_session_does_not_cause_integration_error() {
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
        mainHandler.postWaiting {
            val conviva = ConvivaAnalyticsIntegration(
                    player,
                    CONVIVA_CUSTOMER_KEY,
                    context,
                    ConvivaConfig().apply {
                        isDebugLoggingEnabled = true
                        gatewayUrl = CONVIVA_GATEWAY_URL
                    },
            )
            conviva.updateContentMetadata(
                    MetadataOverrides().apply {
                        applicationName = "Bitmovin Android Conviva integration test app"
                        viewerId = "testViewerId"
                        assetName = "warning_outside_of_active_conviva_session_does_not_cause_integration_error"
                    }
            )
        }

        mainHandler.postWaiting {
            player.load(Sources.Dash.basic)
        }
        player.expectEvent<PlayerEvent.TimeChanged> { it.time > 10.0 }
        mainHandler.postWaiting {
            player.unload() // Unloading the player ends the active conviva session
        }
        runBlocking { delay(500) } // Give conviva some time to end the session
        player.callAndExpectEvent<PlayerEvent.Warning>(block = {
            player.play() // Calling "play" outside of an active playback session triggers a warning
        })
        runBlocking { delay(500) } // Give Conviva some time to report the event
    }
}
