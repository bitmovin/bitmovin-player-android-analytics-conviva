package com.bitmovin.analytics.conviva.testapp

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bitmovin.analytics.conviva.ConvivaAnalytics
import com.bitmovin.analytics.conviva.ConvivaConfiguration
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.config.advertising.AdItem
import com.bitmovin.player.config.advertising.AdSource
import com.bitmovin.player.config.advertising.AdSourceType
import com.bitmovin.player.config.media.DASHSource
import com.bitmovin.player.config.media.SourceItem
import com.bitmovin.player.config.quality.VideoQuality
import com.conviva.api.*
import com.conviva.api.player.PlayerStateManager
import io.mockk.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom
import java.util.*

@RunWith(AndroidJUnit4::class)
class ConvivaAnalyticsTest {
    var convivaConfig: ConvivaConfiguration? = null
    var convivaAnalytics: ConvivaAnalytics? = null
    var clientSettingsMock: ClientSettings? = null
    var clientMock: Client? = null
    var playerStateManagerMock: PlayerStateManager? = null

    val DEFAULT_DASH_VOD_SOURCE = SourceItem(DASHSource("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd"))
    val DEFAULT_DASH_LIVE_SOURCE = SourceItem(DASHSource("https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd"))
    val INVALID_DASH_VOD_SOURCE = SourceItem(DASHSource("https://bitmovin.com"))

    val VMAP_PREROLL_SINGLE_TAG = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator="
    val VMAP_PREROLL_SINGLE_AD_ITEM = AdItem("", AdSource(AdSourceType.IMA, VMAP_PREROLL_SINGLE_TAG))

    var CONVIVA_SESSION_ID = 1
    val PLAYER_TYPE = "Bitmovin Player Android"
    val PLAYER_VERSION = "2.67.0"
    val CUSTOM_EVENT_NAME = "CUSTOM_APPLICATION_EVENT"
    val CUSTOM_EVENT_ATTRIBUTES: HashMap<String, Any> = mutableMapOf(
        "key" to "value"
    ) as HashMap<String, Any>
    val CUSTOM_ERROR_MESSAGE = "CUSTOM_ERROR_MESSAGE"

    @Before
    fun setup() {
        // Create your ConvivaConfiguration object
        convivaConfig = ConvivaConfiguration()
        //convivaConfig!!.gatewayUrl = "https://bitmovin.com"
        convivaConfig!!.isDebugLoggingEnabled = true
    }

    @Before
    fun tearDown() {
        convivaAnalytics?.let { it.endSession() }
        convivaAnalytics = null

        // Unmock mocked objects
        tearDownMocks()
    }

    fun mockClientSettingsObject() : ClientSettings {
        var clientSettings = ClientSettings("customerKey")
        if (convivaConfig?.getGatewayUrl() != null) {
            clientSettings?.gatewayUrl = convivaConfig?.getGatewayUrl()
        }
        mockkObject(clientSettings)
        return clientSettings;
    }

    fun mockClientObject(context: Context, clientSettings: ClientSettings) : Client {
        val androidSystemInterface = AndroidSystemInterfaceFactory.buildSecure(context)
        val systemSettings = SystemSettings()
        systemSettings.allowUncaughtExceptions = false

        if (convivaConfig?.isDebugLoggingEnabled() == true) {
            systemSettings.logLevel = SystemSettings.LogLevel.DEBUG
        }

        val androidSystemFactory = SystemFactory(androidSystemInterface, systemSettings)
        var client = Client(clientSettings, androidSystemFactory)
        mockkObject(client)
        return client;
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
        every { clientSettingsMock!!.isInitialized } returns true
        every { clientMock!!.isInitialized } returns true
        every { clientMock!!.createSession(any()) } returns CONVIVA_SESSION_ID
        every { clientMock!!.playerStateManager } returns playerStateManagerMock
    }

    fun tearDownMocks () {
        clientMock?.let { unmockkObject(it) }
        clientSettingsMock?.let { unmockkObject(it) }
        playerStateManagerMock?.let { unmockkObject(it)}
    }

    fun createConvivaAnalyticsObject(activity: MainActivity) : ConvivaAnalytics {
        // Setup mocks and create ConvivaAnalytics using mock objects
        setupMocks(activity.applicationContext)
        return ConvivaAnalytics(
            activity.bitmovinPlayer!!,
            "test",
            activity.applicationContext,
            convivaConfig,
            clientMock)
    }

    fun defaultMetadataOverrides() : MetadataOverrides {
        val metadata = MetadataOverrides()
        metadata.applicationName = "Bitmovin Android Conviva test app"
        metadata.viewerId = "awesomeViewerId"
        val customInternTags: MutableMap<String, String> = HashMap()
        customInternTags["contentType"] = "Episode"
        metadata.custom = customInternTags
        metadata.assetName = "Conviva Analytics Test Asset"
        return metadata
    }

    @Test
    fun test_implicitSessionStartOnManualPlay() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Create mock objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                activity.bitmovinPlayer!!.play()
                verifyOrder {
                    clientMock?.createSession(any())
                    clientMock?.playerStateManager
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                    clientMock?.attachPlayer(CONVIVA_SESSION_ID, any())
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())

                }
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerType(PLAYER_TYPE)
                    playerStateManagerMock?.setPlayerVersion(PLAYER_VERSION)
                    playerStateManagerMock?.setClientMeasureInterface(any())
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_implicitSessionStartAutoPlay() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Create mock objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                verifyOrder{
                    clientMock?.createSession(any())
                    clientMock?.playerStateManager
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                    clientMock?.attachPlayer(CONVIVA_SESSION_ID, any())
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                }
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerType(PLAYER_TYPE)
                    playerStateManagerMock?.setPlayerVersion(PLAYER_VERSION)
                    playerStateManagerMock?.setClientMeasureInterface(any())
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_explicitSessionStartManualPlay() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)

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
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                clearMocks(clientMock!!)
                clearMocks(playerStateManagerMock!!)
                activity.bitmovinPlayer!!.play()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                verifyOrder {
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                }
                verify {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_explicitSessionStartAutoPlay() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()
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
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                clearMocks(clientMock!!)
                clearMocks(playerStateManagerMock!!)
                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                verifyOrder {
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                }
                verify {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_pauseResume() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Initialize Conviva session
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        // Pause and
        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                activity.bitmovinPlayer!!.pause()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify(atLeast=1) {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PAUSED)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                activity.bitmovinPlayer!!.play()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify(exactly=1) {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_VodSeek() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                activity.bitmovinPlayer!!.seek(120.0)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerSeekStart(120000)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerSeekEnd()
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_liveTimeshift() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_LIVE_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                activity.bitmovinPlayer!!.timeShift(activity.bitmovinPlayer!!.maxTimeShift)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerSeekStart(-1)
                    playerStateManagerMock?.setPlayerSeekEnd()
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_muteUnmute() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(clientMock!!)
                activity.bitmovinPlayer!!.mute()
                Thread.sleep(1000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify {
                    val eventName: String? = MutedEvent::class.simpleName
                    clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, "on$eventName", any())
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(clientMock!!)
                activity.bitmovinPlayer!!.unmute()
                Thread.sleep(1000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify {
                    val eventName: String? = UnmutedEvent::class.simpleName
                    clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, "on$eventName", any())
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_bitrateChange() {
        var switchToVideoQuality: VideoQuality? = null
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                clearMocks(clientMock!!)
                val videoQualityArr = activity.bitmovinPlayer!!.availableVideoQualities
                val currVideoQuality = activity.bitmovinPlayer!!.videoQuality
                switchToVideoQuality = videoQualityArr[0]
                for (quality in videoQualityArr) {
                    if (quality.bitrate < switchToVideoQuality!!.bitrate && quality.id !== currVideoQuality!!.id) {
                        switchToVideoQuality = quality
                    }
                }
                activity.bitmovinPlayer!!.setVideoQuality(switchToVideoQuality?.id)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder() {
                    playerStateManagerMock?.setBitrateKbps(switchToVideoQuality!!.bitrate / 1000)
                    playerStateManagerMock?.setVideoHeight(switchToVideoQuality!!.height)
                    playerStateManagerMock?.setVideoWidth(switchToVideoQuality!!.width)
                }
                verify {
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_sessionEndOnPlaybackFinished() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                clearMocks(clientMock!!)
                activity.bitmovinPlayer!!.seek(activity.bitmovinPlayer!!.duration - 2)
                Thread.sleep(4000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                }
                verifyOrder {
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_sessionEndOnSourceUnloaded() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verifyOrder {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                clearMocks(clientMock!!)
                activity.bitmovinPlayer!!.unload()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify(inverse = true) {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                }
                verifyOrder {
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_errorAfterSessionStart() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Verify session start
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
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                clearMocks(clientMock!!)
                clearMocks(playerStateManagerMock!!)
                activity.bitmovinPlayer!!.load(INVALID_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Verify session is not started again
                verifyAll(inverse=true) {
                    clientMock?.createSession(any())
                    clientMock?.attachPlayer(CONVIVA_SESSION_ID, any())
                }
                verifyAll(inverse=true) {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }

                // Verify error is reported to Conviva and session is ended
                verifyOrder {
                    // Conviva error is reported
                    clientMock?.reportError(CONVIVA_SESSION_ID, any(), Client.ErrorSeverity.FATAL)

                    // Conviva session is ended
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_errorBeforeSessionStart() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)
                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Load invalid asset without initializing Conviva session explicitly
                activity.bitmovinPlayer!!.load(INVALID_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Verify session is started before sending error event
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
                // Verify playback is not reported as buffering and playing
                verifyAll(inverse=true) {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
                // Verify error is reported to Conviva and session is ended
                verifyOrder {
                    // Conviva error is reported
                    clientMock?.reportError(CONVIVA_SESSION_ID, any(), Client.ErrorSeverity.FATAL)

                    // Conviva session is ended
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_explicitSessionEnd() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()

                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
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
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.onActivity { activity: MainActivity ->
            try {
                clearMocks(playerStateManagerMock!!)
                clearMocks(clientMock!!)
                convivaAnalytics?.endSession()
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                verify(inverse = true) {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.STOPPED)
                }
                verifyOrder {
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_customApplicationEvent() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Verify that custom events are not sent before Conviva session is initialized
                convivaAnalytics?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME)
                convivaAnalytics?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME, CUSTOM_EVENT_ATTRIBUTES)
                verifyAll(inverse = true) {
                    clientMock?.sendCustomEvent(Client.NO_SESSION_KEY, CUSTOM_EVENT_NAME,
                        HashMap());
                    clientMock?.sendCustomEvent(Client.NO_SESSION_KEY, CUSTOM_EVENT_NAME,
                        CUSTOM_EVENT_ATTRIBUTES);
                }

                // Verify that custom events are sent after Conviva session is initialized
                convivaAnalytics!!.initializeSession()
                convivaAnalytics?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME)
                convivaAnalytics?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME, CUSTOM_EVENT_ATTRIBUTES)
                verifyOrder() {
                    clientMock?.sendCustomEvent(Client.NO_SESSION_KEY, CUSTOM_EVENT_NAME,
                        HashMap());
                    clientMock?.sendCustomEvent(Client.NO_SESSION_KEY, CUSTOM_EVENT_NAME,
                        CUSTOM_EVENT_ATTRIBUTES);
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }

    @Test
    fun test_customPlaybackEvent() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Verify that custom events are not sent before Conviva session is initialized
                convivaAnalytics?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME)
                convivaAnalytics?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME, CUSTOM_EVENT_ATTRIBUTES)
                verifyOrder(inverse = true) {
                    clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, CUSTOM_EVENT_NAME,
                        HashMap());
                    clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, CUSTOM_EVENT_NAME,
                        CUSTOM_EVENT_ATTRIBUTES);
                }

                // Verify that custom events are sent after Conviva session is initialized
                convivaAnalytics!!.initializeSession()
                convivaAnalytics?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME)
                convivaAnalytics?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME, CUSTOM_EVENT_ATTRIBUTES)
                verifyOrder() {
                    clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, CUSTOM_EVENT_NAME,
                        HashMap());
                    clientMock?.sendCustomEvent(CONVIVA_SESSION_ID, CUSTOM_EVENT_NAME,
                        CUSTOM_EVENT_ATTRIBUTES);
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.close()
    }

    @Test
    fun test_customErrorEvent() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Verify that custom error events are not sent before Conviva session is initialized
                convivaAnalytics?.reportPlaybackDeficiency(CUSTOM_ERROR_MESSAGE, Client.ErrorSeverity.WARNING, false)
                verify(inverse = true) {
                    clientMock?.reportError(any(), any(), any());
                }
                convivaAnalytics?.reportPlaybackDeficiency(CUSTOM_ERROR_MESSAGE, Client.ErrorSeverity.FATAL, true)
                verify(inverse = true) {
                    clientMock?.reportError(any(), any(), any());
                }

                // Verify that custom error events are sent after Conviva session is initialized
                // but Conviva session is not ended when endSession is passed as false
                convivaAnalytics!!.initializeSession()
                clearMocks(clientMock!!)
                convivaAnalytics?.reportPlaybackDeficiency(CUSTOM_ERROR_MESSAGE, Client.ErrorSeverity.WARNING, false)
                verify(exactly = 1) {
                    clientMock?.reportError(CONVIVA_SESSION_ID, CUSTOM_ERROR_MESSAGE, Client.ErrorSeverity.WARNING);
                }
                verifyAll(inverse = true) {
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }

                // Verify that custom error events are sent after Conviva session is initialized
                // and Conviva session is also ended when endSession is passed as true
                clearMocks(clientMock!!)
                clearMocks(playerStateManagerMock!!)
                convivaAnalytics?.reportPlaybackDeficiency(CUSTOM_ERROR_MESSAGE, Client.ErrorSeverity.FATAL, true)
                verifyOrder() {
                    clientMock?.reportError(CONVIVA_SESSION_ID, CUSTOM_ERROR_MESSAGE, Client.ErrorSeverity.FATAL);
                    clientMock?.detachPlayer(CONVIVA_SESSION_ID)
                    clientMock?.cleanupSession(CONVIVA_SESSION_ID)
                    clientMock?.releasePlayerStateManager(playerStateManagerMock)
                }

            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.close()
    }

    @Test
    fun test_adEventsVmapPrerollSingleAd() {
        val launchIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivity::class.java
        )
        launchIntent.putExtra(MainActivity.AUTOPLAY_KEY, true)
        launchIntent.putExtra(MainActivity.VMAP_KEY, VMAP_PREROLL_SINGLE_TAG)
        val activityScenario = ActivityScenario.launch<MainActivity>(launchIntent)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Mock Conviva SDK objects
                convivaAnalytics = createConvivaAnalyticsObject(activity)

                // Set default metadata overrides
                convivaAnalytics!!.updateContentMetadata(defaultMetadataOverrides())

                // Call play explicitly and check that Conviva session should be
                // initialized/started implicitly
                convivaAnalytics!!.initializeSession()
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                clearMocks(clientMock!!)
                clearMocks(playerStateManagerMock!!)
                // Load DASH source
                activity.bitmovinPlayer!!.load(DEFAULT_DASH_VOD_SOURCE)
                Thread.sleep(2000)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.onActivity { activity: MainActivity ->
            try {
                // Load invalid DASH source
                verifyOrder {
                    clientMock?.updateContentMetadata(CONVIVA_SESSION_ID, any())
                    clientMock?.adStart(
                        CONVIVA_SESSION_ID,
                        Client.AdStream.SEPARATE,
                        Client.AdPlayer.CONTENT,
                        Client.AdPosition.PREROLL
                    )
                }
                verify {
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.BUFFERING)
                    playerStateManagerMock?.setPlayerState(PlayerStateManager.PlayerState.PLAYING)
                }
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }

        activityScenario.close()
    }
}