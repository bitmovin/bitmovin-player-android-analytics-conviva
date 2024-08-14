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
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.network.HttpRequestType
import com.bitmovin.player.api.network.NetworkConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture

/**
 * This test class does not verify any specific behavior, but rather can be used to validate the
 * integration against the [Conviva Touchstone integration test tool](https://touchstone.conviva.com/).
 */
@RunWith(AndroidJUnit4::class)
class MetricReportingTests {
    /**
     * Plays the first 5 seconds of a stream with a attached [ConvivaAnalyticsIntegration].
     * Additionally injects average bitrate to the HLS manifest in order to report it.
     */
    @Test
    fun reports_average_bitrate_and_peak_bitrate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mainHandler = Handler(context.mainLooper)
        val player = mainHandler.postWaiting {
            val player = Player(
                context,
                PlayerConfig(
                    key = BITMOVIN_PLAYER_LICENSE_KEY,
                    playbackConfig = PlaybackConfig(
                        isAutoplayEnabled = true,
                    ),
                    networkConfig = NetworkConfig(preprocessHttpResponseCallback = { type, response ->
                        if (type != HttpRequestType.ManifestHlsMaster) {
                            return@NetworkConfig null
                        }
                        val body = response.body.decodeToString()

                        val regex = "BANDWIDTH=([0-9]+),".toRegex()
                        val newBody = regex.replace(body) {
                            val bandwidth = it.groups[1]!!.value.toInt()
                            "BANDWIDTH=${bandwidth},AVERAGE-BANDWIDTH=${bandwidth - 100_000},"
                        }

                        CompletableFuture.completedFuture(response.copy(body = newBody.encodeToByteArray()))
                    })
                ),
                analyticsConfig = AnalyticsPlayerConfig.Disabled,
            )
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
            player
        }

        mainHandler.postWaiting { player.load(Sources.Hls.basic) }
        player.expectEvent<PlayerEvent.TimeChanged> { it.time > 5.0 }

        // pause player for a second and resume playback
        mainHandler.postWaiting { player.pause() }
        runBlocking { delay(1000) }
        mainHandler.postWaiting { player.play() }
        runBlocking { delay(1000) }

        mainHandler.postWaiting { player.destroy() }
        runBlocking { delay(1000) }
    }
}
