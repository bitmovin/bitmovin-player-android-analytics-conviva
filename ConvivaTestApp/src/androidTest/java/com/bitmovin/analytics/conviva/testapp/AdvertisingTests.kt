package com.bitmovin.analytics.conviva.testapp

import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration
import com.bitmovin.analytics.conviva.ConvivaConfig
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventEmitter
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val BITMOVIN_PLAYER_LICENSE_KEY = "YOUR_LICENSE_KEY"
private const val CONVIVA_CUSTOMER_KEY = "YOUR-CUSTOMER-KEY"
private const val CONVIVA_GATEWAY_URL = "YOUR-GATEWAY-URL"

private const val VMAP_PREROLL_SINGLE_TAG = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator="
private val ART_OF_MOTION_DASH = SourceConfig(
        url = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd",
        type = SourceType.Dash,
        title = "Art of Motion Test Stream",
)

/**
 * This test class does not verify any specific behavior, but rather can be used to validate the
 * integration against the [Conviva Touchstone integration test tool](https://touchstone.conviva.com/).
 */
@RunWith(AndroidJUnit4::class)
class AdvertisingTests {
    /**
     * Plays the first 5 seconds of a stream with a VMAP ad that includes a single pre-roll with a
     * attached [ConvivaAnalyticsIntegration].
     * Ad playback is paused and resumed to test for according events.
     */
    @Test
    fun reports_ad_analytics_for_IMA_pre_roll_ad() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mainHandler = Handler(context.mainLooper)
        val player = mainHandler.postWaiting {
            val player = Player(
                    context,
                    PlayerConfig(
                            key = BITMOVIN_PLAYER_LICENSE_KEY,
                            advertisingConfig = AdvertisingConfig(
                                    AdItem(AdSource(AdSourceType.Ima, VMAP_PREROLL_SINGLE_TAG)),
                            ),
                            playbackConfig = PlaybackConfig(
                                    isAutoplayEnabled = true,
                            ),
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

        mainHandler.postWaiting { player.load(ART_OF_MOTION_DASH) }
        player.expectEvent<PlayerEvent.TimeChanged> { it.time > 5.0 }

        // pause player for a second and resume playback
        mainHandler.postWaiting { player.pause() }
        runBlocking { delay(1000) }
        mainHandler.postWaiting { player.play() }

        // wait for the ad break to finish and play main content for five more seconds
        player.expectEvent<PlayerEvent.AdBreakFinished>()
        player.expectEvent<PlayerEvent.TimeChanged> { it.time > 5.0 }

        mainHandler.postWaiting { player.destroy() }
        runBlocking { delay(1000) }
    }
}

/**
 * Subscribes to an [Event] on the [Player] and suspends until the event is emitted.
 * Optionally a [condition] can be provided to filter the emitted events.
 */
private inline fun <reified T : Event> EventEmitter<Event>.expectEvent(
        crossinline condition: (T) -> Boolean = { true }
) = runBlocking {
    suspendCoroutine { continuation ->
        lateinit var action: ((T) -> Unit)
        action = {
            if (condition(it)) {
                off(action)
                continuation.resume(Unit)
            }
        }
        on<T>(action)
    }
}

/**
 * Posts a [block] of code to the main thread and suspends until it is executed.
 */
private inline fun <T> Handler.postWaiting(crossinline block: () -> T) = runBlocking {
    suspendCoroutine { continuation ->
        post { continuation.resume(block()) }
    }
}
