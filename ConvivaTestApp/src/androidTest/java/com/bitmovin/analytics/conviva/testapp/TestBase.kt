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
import com.conviva.api.player.PlayerStateManager
import io.mockk.*
import org.junit.Assert
import org.junit.Before
import org.junit.After
import java.security.SecureRandom
import java.util.HashMap

open class TestBase {
    lateinit var convivaConfig: ConvivaConfig
    var convivaAnalytics: ConvivaAnalyticsIntegration? = null
    var clientSettingsMock: ClientSettings? = null
    var clientMock: Client? = null
    var playerStateManagerMock: PlayerStateManager? = null
    lateinit var activityScenario: ActivityScenario<MainActivity>

    var CONVIVA_SESSION_ID = 1
    val CUSTOM_EVENT_NAME = "CUSTOM_APPLICATION_EVENT"
    val CUSTOM_EVENT_ATTRIBUTES: HashMap<String, Any> = mutableMapOf(
        "key" to "value"
    ) as HashMap<String, Any>
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
        convivaAnalytics?.let { it.endSession() }

        // Unmock mocked objects
        tearDownMocks()
        activityScenario.close()
    }

    fun mockClientSettingsObject() : ClientSettings {
        var clientSettings = ClientSettings("customerKey")
        if (convivaConfig.getGatewayUrl() != null) {
            clientSettings.gatewayUrl = convivaConfig.getGatewayUrl()
        }
        mockkObject(clientSettings)
        return clientSettings
    }

    fun mockClientObject(
        context: Context,
        clientSettings: ClientSettings
    ) : Client {
        val androidSystemInterface = AndroidSystemInterfaceFactory.buildSecure(context)
        val systemSettings = SystemSettings()
        systemSettings.allowUncaughtExceptions = false

        if (convivaConfig.isDebugLoggingEnabled() == true) {
            systemSettings.logLevel = SystemSettings.LogLevel.DEBUG
        }

        val androidSystemFactory = SystemFactory(androidSystemInterface, systemSettings)
        var client = Client(clientSettings, androidSystemFactory)
        mockkObject(client)
        return client
    }

    fun mockPlayerStateManagerObject(client: Client) : PlayerStateManager {
        var playerStateManager = client.playerStateManager
        mockkObject(playerStateManager)
        return playerStateManager
    }

    fun setupMocks(context: Context) {
        clientSettingsMock = mockClientSettingsObject()
        clientMock = mockClientObject(context, clientSettingsMock!!)
        playerStateManagerMock = mockPlayerStateManagerObject(clientMock!!)

        // Stub method return values
        CONVIVA_SESSION_ID = Math.abs(SecureRandom().nextInt())
        every { clientSettingsMock?.isInitialized } returns true
        every { clientMock?.isInitialized } returns true
        every { clientMock?.createSession(any()) } returns CONVIVA_SESSION_ID
        every { clientMock?.playerStateManager } returns playerStateManagerMock
    }

    fun tearDownMocks () {
        clientMock?.let { unmockkObject(it) }
        clientSettingsMock?.let { unmockkObject(it) }
        playerStateManagerMock?.let { unmockkObject(it) }
    }

    fun createConvivaAnalyticsObject(activity: MainActivity) : ConvivaAnalyticsIntegration {
        // Setup mocks and create ConvivaAnalytics using mock objects
        setupMocks(activity.applicationContext)
        return ConvivaAnalyticsIntegration(
                activity.bitmovinPlayer,
                "test",
                activity.applicationContext,
                convivaConfig,
                clientMock)
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
        streamType: ContentMetadata.StreamType? = null,
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

    fun expectedContentMetadata(
        source: Source,
        streamType: ContentMetadata.StreamType,
        duration: Int,
        overrideMetadata: MetadataOverrides,
        overrideCustom: Boolean = false
    ) : ContentMetadata {
        val contentMetadata = ContentMetadata()
        contentMetadata.applicationName = overrideMetadata.applicationName
        contentMetadata.viewerId = overrideMetadata.viewerId
        contentMetadata.assetName = overrideMetadata.assetName
        contentMetadata.defaultResource = null
        contentMetadata.encodedFrameRate = -1
        contentMetadata.defaultBitrateKbps = -1
        contentMetadata.isOfflinePlayback = false
        contentMetadata.custom = HashMap()
        if (overrideCustom) {
            contentMetadata.streamUrl = overrideMetadata.streamUrl ?: source.config.url
            contentMetadata.streamType = overrideMetadata.streamType ?: streamType
            contentMetadata.duration = overrideMetadata.duration ?: duration
            overrideMetadata.custom.forEach { s, s2 ->
                contentMetadata.custom[s] = s2
            }
            contentMetadata.custom["streamType"] = overrideMetadata.custom["streamType"] ?: source.config.type.toString()
            contentMetadata.custom["integrationVersion"] = overrideMetadata.custom["integrationVersion"] ?: com.bitmovin.analytics.conviva.BuildConfig.VERSION_NAME
        } else {
            contentMetadata.streamUrl = source.config.url
            contentMetadata.streamType = streamType
            contentMetadata.duration = duration
            contentMetadata.custom["streamType"] = source.config.type.toString()
            contentMetadata.custom["integrationVersion"] = com.bitmovin.analytics.conviva.BuildConfig.VERSION_NAME
        }
        return contentMetadata
    }

    fun MockKMatcherScope.metadataEq(expectedMetatada: ContentMetadata) = match<ContentMetadata>  {
        if (it.assetName == expectedMetatada.assetName &&
            it.applicationName == expectedMetatada.applicationName &&
            it.viewerId == expectedMetatada.viewerId &&
            it.streamType == expectedMetatada.streamType &&
            it.streamUrl == expectedMetatada.streamUrl &&
            it.duration == expectedMetatada.duration &&
            it.defaultResource == expectedMetatada.defaultResource &&
            it.encodedFrameRate == expectedMetatada.encodedFrameRate &&
            it.custom.equals(expectedMetatada.custom)) {
            true
        } else {
            false
        }
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
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics?.updateContentMetadata(metadataOverrides)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        return activityScenario
    }

    fun initializeSession(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            convivaAnalytics?.initializeSession()
            Thread.sleep(1000)
        }
    }

    fun verifySessionIntialization(activityScenario: ActivityScenario<MainActivity>) {
        activityScenario.onActivity { activity: MainActivity ->
            verifyOrder {
                clientMock?.createSession(any())
                clientMock?.playerStateManager
                clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                clientMock?.attachPlayer(CONVIVA_SESSION_ID, any())
            }
            verifyOrder {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                playerStateManagerMock?.setPlayerType(PLAYER_TYPE)
                playerStateManagerMock?.setPlayerVersion(PLAYER_VERSION)
                playerStateManagerMock?.setClientMeasureInterface(any())
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
            verify() {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
            }
        }
    }

    fun verifyPlayingWithMetadata(
        activityScenario: ActivityScenario<MainActivity>,
        source: Source,
        streamType: ContentMetadata.StreamType,
        streamDuration: Int,
        metadata: MetadataOverrides,
        overrideCustom: Boolean = false
    ) {
        activityScenario.onActivity { activity: MainActivity ->
            verify {
                clientMock?.updateContentMetadata(CONVIVA_SESSION_ID,
                    metadataEq(
                        expectedContentMetadata(
                            source = source,
                            streamType = streamType,
                            duration = streamDuration,
                            overrideMetadata = metadata,
                            overrideCustom = overrideCustom
                        )
                    )
                )
            }

            verifyOrder {
                playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
            }
        }
    }
}
