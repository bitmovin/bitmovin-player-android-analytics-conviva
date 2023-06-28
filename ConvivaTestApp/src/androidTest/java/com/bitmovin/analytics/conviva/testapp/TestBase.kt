package com.bitmovin.analytics.conviva.testapp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration
import com.bitmovin.analytics.conviva.ConvivaConfig
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
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
    var convivaAnalyticsMock: ConvivaAnalytics? = null
    lateinit var activityScenario: ActivityScenario<MainActivity>

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
    val TIMEOUT = 4000

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

    fun mockConvivaAnalyticsObject() {
        mockkStatic(ConvivaAnalytics::class)
    }

    fun setupMocks(context: Context) {
        videoAnalyticsMock = mockVideoAnalyticsObject(context)
        mockConvivaAnalyticsObject()
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
                convivaConfig, videoAnalyticsMock))
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
            customTags.forEach { (s: String, s2: String) ->
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
    ) : MutableMap<String, Any> {
        val contentInfo = mutableMapOf<String, Any>()
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
            overrideMetadata.custom.forEach { (s: String, s2: String) ->
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
    fun MockKMatcherScope.metadataEq(expectedMetadata: MetadataOverrides) = match<MetadataOverrides>  {
        it.assetName == expectedMetadata.assetName &&
        it.applicationName == expectedMetadata.applicationName &&
        it.viewerId == expectedMetadata.viewerId &&
        it.streamType == expectedMetadata.streamType &&
        it.streamUrl == expectedMetadata.streamUrl &&
        it.duration == expectedMetadata.duration &&
        it.encodedFrameRate == expectedMetadata.encodedFrameRate &&
        it.custom.equals(expectedMetadata.custom)
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
            Thread.sleep(TIMEOUT.toLong())
        }
    }

    fun verifySessionInitialization(activityScenario: ActivityScenario<MainActivity>) {
        verifySessionInitialization(activityScenario, null);
    }

    // Implicit session tests do not actually initialize the session until play starts, in the new SDK
    fun verifySessionInitialization(activityScenario: ActivityScenario<MainActivity>, metadata: MetadataOverrides?, implicit: Boolean = false) {
        activityScenario.onActivity { _: MainActivity ->
            verifyOrder {
                if(metadata != null) {
                    convivaAnalyticsIntegration?.updateContentMetadata(metadata)
                }
                if(!implicit) {
                    convivaAnalyticsIntegration?.initializeSession()

                    if (metadata != null) {
                        convivaAnalyticsIntegration?.updateContentMetadata(metadata)
                    }
                }
            }
        }
    }

    fun loadSource(activityScenario: ActivityScenario<MainActivity>, source: Source) {
        activityScenario.onActivity { activity: MainActivity ->
            activity.bitmovinPlayer.load(source)
        }
        Thread.sleep(TIMEOUT.toLong())
    }

    fun playSource(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            activity.bitmovinPlayer.play()
        }
    }

    fun verifyPlaying(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING);
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
                val rawMetadata = expectedContentMetadata(
                    source = source,
                    streamType = streamType,
                    duration = streamDuration,
                    overrideMetadata = metadata,
                    overrideCustom = overrideCustom
                )


                metadata.assetName = rawMetadata[ConvivaSdkConstants.ASSET_NAME] as String?
                metadata.applicationName = rawMetadata[ConvivaSdkConstants.PLAYER_NAME] as String?
                metadata.viewerId = rawMetadata[ConvivaSdkConstants.VIEWER_ID] as String?
                metadata.streamType = if (rawMetadata[ConvivaSdkConstants.IS_LIVE] as Boolean) ConvivaSdkConstants.StreamType.LIVE else ConvivaSdkConstants.StreamType.VOD
                metadata.streamUrl = rawMetadata[ConvivaSdkConstants.STREAM_URL] as String?
                metadata.duration = rawMetadata[ConvivaSdkConstants.DURATION] as Int?
                metadata.encodedFrameRate = rawMetadata[ConvivaSdkConstants.ENCODED_FRAMERATE] as Int?

                convivaAnalyticsIntegration?.updateContentMetadata(metadataEq ( metadata ))
            }



            verify(atLeast = 1) {
                videoAnalyticsMock?.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.PLAYING);
            }
        }
    }
}
