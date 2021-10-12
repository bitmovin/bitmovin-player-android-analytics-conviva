package com.bitmovin.analytics.conviva.testapp

import androidx.test.core.app.ActivityScenario
import com.bitmovin.analytics.conviva.ConvivaAnalytics
import com.bitmovin.analytics.conviva.ConvivaConfiguration
import com.bitmovin.analytics.conviva.MetadataOverrides
import com.conviva.api.Client
import io.mockk.*

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.api.ClientSettings
import org.junit.Before
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConvivaAnalyticsTest {
    var customMetadataOverride: MetadataOverrides? = null
    var convivaConfig: ConvivaConfiguration? = null
    var convivaAnalytics: ConvivaAnalytics? = null

    @Before
    fun setup() {
        // Create your ConvivaConfiguration object
        convivaConfig = ConvivaConfiguration()
        convivaConfig!!.gatewayUrl = "https://bitmovin.com"
        convivaConfig!!.isDebugLoggingEnabled = true
        customMetadataOverride = MetadataOverrides()
    }

    @Before
    fun tearDown() {
        convivaAnalytics = null
    }

    @Test
    fun test_createConvivaAnalyticsWithValidPlayer() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                mockkConstructor(ClientSettings::class)
                mockkConstructor(Client::class)
                every {
                    anyConstructed<ClientSettings>().isInitialized // Mocks the constructor which takes a String
                } returns true
                every {
                    anyConstructed<Client>().isInitialized // Mocks the constructor which takes a String
                } returns true
                // Create ConvivaAnalytics
                convivaAnalytics = ConvivaAnalytics(
                        activity.bitmovinPlayer,
                    "test",
                        activity.applicationContext,
                        convivaConfig)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.onActivity { activity: MainActivity? -> convivaAnalytics = null }
        activityScenario.close()
        unmockkConstructor(Client::class)
        unmockkConstructor(ClientSettings::class)
    }

    @Test
    fun test_setOverrideMetadataBeforeSessionStarted() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.onActivity { activity: MainActivity ->
            try {
                mockkConstructor(ClientSettings::class)
                mockkConstructor(Client::class)
                every {
                    anyConstructed<ClientSettings>().isInitialized // Mocks the constructor which takes a String
                } returns true
                every {
                    anyConstructed<Client>().isInitialized // Mocks the constructor which takes a String
                } returns true
                // Create ConvivaAnalytics
                convivaAnalytics = ConvivaAnalytics(
                        activity.bitmovinPlayer,
                    "test",
                        activity.applicationContext,
                        convivaConfig)
                customMetadataOverride = MetadataOverrides()
                convivaAnalytics!!.updateContentMetadata(customMetadataOverride)
            } catch (e: Exception) {
                // Expectation is to not receive any exception
                Assert.assertTrue("Received unexpected exception: $e", false)
            }
        }
        activityScenario.onActivity { activity: MainActivity? -> convivaAnalytics = null }
        activityScenario.close()
        unmockkConstructor(Client::class)
        unmockkConstructor(ClientSettings::class)
    }

    @Test
    fun test_trackSessionStartManualPlay() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.onActivity { activity: MainActivity ->
            mockkConstructor(ClientSettings::class)
            mockkConstructor(Client::class)
            every {
                anyConstructed<ClientSettings>().isInitialized // Mocks the constructor which takes a String
            } returns true
            every {
                anyConstructed<Client>().isInitialized // Mocks the constructor which takes a String
            } returns true
            // Create ConvivaAnalytics
            convivaAnalytics = ConvivaAnalytics(
                    activity.bitmovinPlayer,
                    "test",
                    activity.applicationContext,
                    convivaConfig)
            customMetadataOverride = MetadataOverrides()
            convivaAnalytics!!.updateContentMetadata(customMetadataOverride)
            activity.bitmovinPlayer.play()
            verify(atLeast = 1, timeout = 5000) { anyConstructed<ClientSettings>().isInitialized }
            verify(atLeast = 1, timeout = 10000) { anyConstructed<Client>().playerStateManager }
        }

        activityScenario.onActivity { activity: MainActivity -> activity.bitmovinPlayer.destroy() }
        activityScenario.close()
        unmockkConstructor(Client::class)
        unmockkConstructor(ClientSettings::class)
    }
}