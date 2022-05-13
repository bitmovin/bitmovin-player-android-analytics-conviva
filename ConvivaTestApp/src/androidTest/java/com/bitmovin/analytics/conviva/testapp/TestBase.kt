package com.bitmovin.analytics.conviva.testapp

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration
import com.bitmovin.analytics.conviva.ConvivaConfig
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import com.conviva.api.*
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import com.conviva.sdk.ConvivaVideoAnalytics
import io.mockk.*
import org.junit.After
import org.junit.Assert
import org.junit.Before

open class TestBase {
    lateinit var convivaConfig: ConvivaConfig
    var convivaAnalyticsIntegration: ConvivaAnalyticsIntegration? = null
    var videoAnalyticsMock: ConvivaVideoAnalytics? = null
    lateinit var activityScenario: ActivityScenario<MainActivity>

    var CONVIVA_SESSION_ID = 1
    val CUSTOM_EVENT_NAME = "CUSTOM_APPLICATION_EVENT"
    val CUSTOM_EVENT_ATTRIBUTES: HashMap<String, String> = mutableMapOf(
        "key" to "value"
    ) as HashMap<String, String>
    val CUSTOM_ERROR_MESSAGE = "CUSTOM_ERROR_MESSAGE"

    val DEFAULT_DASH_VOD_SOURCE = Source.create(SourceConfig("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd", SourceType.Dash))
    val DEFAULT_DASH_VOD_SOURCE_DURATION = 210
    val DEFAULT_DASH_LIVE_SOURCE = Source.create(SourceConfig("https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd", SourceType.Dash))
    val DEFAULT_DASH_LIVE_SOURCE_DURATION = 0
    val INVALID_DASH_VOD_SOURCE = Source.create(SourceConfig("https://bitmovin.com", SourceType.Dash))

    val PLAYER_TYPE = "Bitmovin Player Android"
    val PLAYER_VERSION = com.bitmovin.player.BuildConfig.VERSION_NAME

    @Before
    fun setup() {
        // Create your ConvivaConfiguration object
        convivaConfig = ConvivaConfig()
        convivaConfig.isDebugLoggingEnabled = true
    }

    @After
    fun tearDown() {
        convivaAnalyticsIntegration?.let { it.endSession() }

        // Unmock mocked objects
        tearDownMocks()
        activityScenario.close()
    }

    fun mockVideoAnalyticsObject(context: Context): ConvivaVideoAnalytics {
        var convivaVideoAnalytics = ConvivaAnalytics.buildVideoAnalytics(context)
        mockkObject(convivaVideoAnalytics)
        return convivaVideoAnalytics
    }

    fun setupMocks(context: Context) {
        videoAnalyticsMock = mockVideoAnalyticsObject(context)
    }

    fun tearDownMocks () {
        videoAnalyticsMock?.let { unmockkObject(it) }
    }

    fun  createConvivaAnalyticsObject(activity: MainActivity) : ConvivaAnalyticsIntegration {
        // Setup mocks and create ConvivaAnalytics using mock objects
        ConvivaAnalytics.init(activity.applicationContext, "test")
        setupMocks(activity.applicationContext)
        return spyk(ConvivaAnalyticsIntegration(
                activity.bitmovinPlayer,
                "test",
                activity.applicationContext,
                convivaConfig, videoAnalyticsMock), recordPrivateCalls = true)
    }

    fun defaultMetadataOverrides() : MetadataOverrides {
        val metadata = MetadataOverrides()
        metadata.applicationName = "Bitmovin Android Conviva test app"
        metadata.viewerId = "awesomeViewerId"
        metadata.assetName = "Conviva Analytics Test Asset"
        return metadata
    }

    fun customMetadataOverrides(
        source: Source? = null,
        streamType: ConvivaSdkConstants.StreamType? = null,
        duration: Int? = null,
        customTags: Map<String, String>? = null
    ) : MetadataOverrides {
        val metadata = MetadataOverrides()
        metadata.applicationName = "Bitmovin Android Conviva test app"
        metadata.viewerId = "awesomeViewerId"
        metadata.assetName = "Conviva Analytics Test Asset"
        metadata.custom = HashMap()
        customTags?.let {
            customTags.forEach { s, s2 ->
                metadata.custom[s] = s2
            }
        }
        source?.let { metadata.streamUrl = it.config.url }
        streamType?.let { metadata.streamType = streamType }
        streamType?.let { metadata.duration = duration }

        return metadata
    }

    // TODO: All references to ContentMetadata are obsolete and we should be using Map<String, String> instead
    fun expectedContentMetadata(
        source: Source,
        streamType: ConvivaSdkConstants.StreamType,
        duration: Int,
        overrideMetadata: MetadataOverrides,
        overrideCustom: Boolean = false
    ) : Map<String, Any> {
        val contentInfo = HashMap<String, Any>()
        contentInfo[ConvivaSdkConstants.PLAYER_NAME] = overrideMetadata.applicationName
        contentInfo[ConvivaSdkConstants.VIEWER_ID] = overrideMetadata.viewerId
        contentInfo[ConvivaSdkConstants.ASSET_NAME] = overrideMetadata.assetName
        contentInfo[ConvivaSdkConstants.ENCODED_FRAMERATE] = -1
        contentInfo[ConvivaSdkConstants.IS_OFFLINE_PLAYBACK] = false
        if (overrideCustom) {
            contentInfo[ConvivaSdkConstants.STREAM_URL] = overrideMetadata.streamUrl ?: source.config.url
            val streamTypeLocal = overrideMetadata.streamType ?: streamType
            contentInfo[ConvivaSdkConstants.IS_LIVE] = streamTypeLocal == ConvivaSdkConstants.StreamType.LIVE;
            contentInfo[ConvivaSdkConstants.DURATION] = overrideMetadata.duration ?: duration
            overrideMetadata.custom.forEach { s, s2 ->
                contentInfo[s] = s2
            }
            contentInfo["streamType"] = overrideMetadata.custom["streamType"] ?: source.config.type.toString()
            contentInfo["integrationVersion"] = overrideMetadata.custom["integrationVersion"] ?: com.bitmovin.analytics.conviva.BuildConfig.VERSION_NAME
        } else {
            contentInfo[ConvivaSdkConstants.STREAM_URL] = source.config.url
            contentInfo[ConvivaSdkConstants.IS_LIVE] = streamType == ConvivaSdkConstants.StreamType.LIVE
            contentInfo[ConvivaSdkConstants.DURATION] = duration
            contentInfo["streamType"] = source.config.type.toString()
            contentInfo["integrationVersion"] = com.bitmovin.analytics.conviva.BuildConfig.VERSION_NAME
        }
        return contentInfo
    }

    // TODO: All references to ContentMetadata are obsolete and we should be using Map<String, String> instead
    fun MockKMatcherScope.metadataEq(expectedMetadata: Map<String, String>) = match<Map<String, String>>  {
        it[ConvivaSdkConstants.ASSET_NAME] == expectedMetadata[ConvivaSdkConstants.ASSET_NAME] &&
        it[ConvivaSdkConstants.PLAYER_NAME] == expectedMetadata[ConvivaSdkConstants.PLAYER_NAME] &&
        it[ConvivaSdkConstants.VIEWER_ID] == expectedMetadata[ConvivaSdkConstants.VIEWER_ID] &&
        it[ConvivaSdkConstants.IS_LIVE] == expectedMetadata[ConvivaSdkConstants.IS_LIVE] &&
        it[ConvivaSdkConstants.STREAM_URL] == expectedMetadata[ConvivaSdkConstants.STREAM_URL] &&
        it[ConvivaSdkConstants.DURATION] == expectedMetadata[ConvivaSdkConstants.DURATION] &&
        it[ConvivaSdkConstants.ENCODED_FRAMERATE] == expectedMetadata[ConvivaSdkConstants.ENCODED_FRAMERATE] &&
        it["streamType"] == expectedMetadata["streamType"] &&
        it["integrationVersion"] == expectedMetadata["integrationVersion"]
    }


    fun setupPlayerActivityForTest(
        autoPlay: Boolean,
        metadataOverrides: MetadataOverrides,
        adTag: String = ""
    ) : ActivityScenario<MainActivity> {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        if (autoPlay) {
            launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        }
        if (adTag != "") {
            launchIntent.putExtra(MainActivity.VMAP_KEY, adTag)
        }
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalyticsIntegration = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalyticsIntegration?.updateContentMetadata(metadataOverrides)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        return activityScenario
    }

    fun initializeSession(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            convivaAnalyticsIntegration?.initializeSession()
            Thread.sleep(1000)
        }
    }

    fun verifySessionInitialization(activityScenario: ActivityScenario<MainActivity>) {
        verifySessionInitialization(activityScenario, null);
    }
    fun verifySessionInitialization(activityScenario: ActivityScenario<MainActivity>, metadata: MetadataOverrides?) {
        activityScenario.onActivity { _: MainActivity ->
            verifyOrder {
                if(metadata != null) {
                    convivaAnalyticsIntegration?.updateContentMetadata(metadata)
                }

                convivaAnalyticsIntegration?.get("createContentMetadata")

                convivaAnalyticsIntegration?.get("buildDynamicContentMetadata")

                convivaAnalyticsIntegration?.get("updateSession")

                convivaAnalyticsIntegration?.get("buildDynamicContentMetadata")
            }
        }
    }

    fun loadSource(activityScenario: ActivityScenario<MainActivity>, source: Source) {
        activityScenario.onActivity { activity: MainActivity ->
            activity.bitmovinPlayer.load(source)
        }
        Thread.sleep(2000)
    }

    fun playSource(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            activity.bitmovinPlayer.play()
        }
    }

    fun verifyPlaying(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING);
            }
        }
    }

    fun verifyPlayingWithMetadata(
        activityScenario: ActivityScenario<MainActivity>,
        source: Source,
        streamType: ConvivaSdkConstants.StreamType,
        streamDuration: Int,
        metadata: MetadataOverrides,
        overrideCustom: Boolean = false
    ) {
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                // TODO: Verify updateContentMetadata in the new form
                convivaAnalyticsIntegration?.updateContentMetadata(
                    metadataEq(
                            expectedContentMetadata(
                                    source = source,
                                    streamType = streamType,
                                    duration = streamDuration,
                                    overrideMetadata = metadata,
                                    overrideCustom = overrideCustom
                            ) as Map<String, String>
                    ) as MetadataOverrides
                )
            }

            verifyOrder {
                // TODO: Do the equivalent verification
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.BUFFERING);
                //playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
            }
        }
    }
}
