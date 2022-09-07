package com.bitmovin.analytics.conviva.testapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.conviva.sdk.ConvivaAnalytics
import com.conviva.sdk.ConvivaSdkConstants
import io.mockk.clearMocks
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomEventsTests: TestBase() {
    @Test
    fun customApplicationEvent() {
        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, defaultMetadataOverrides())

        // initialize and verify session
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario)

        // In the latest Conviva SDK, there is no problem with reporting app events before
        // a session is opened, as shown here.
        activityScenario.onActivity { activity: MainActivity ->
            convivaAnalyticsIntegration?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME)
            convivaAnalyticsIntegration?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)

            verifyOrder() {
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap())
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)
            }
        }

        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        activityScenario.onActivity { activity: MainActivity ->
            convivaAnalyticsIntegration?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME)
            convivaAnalyticsIntegration?.sendCustomApplicationEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)
            verifyOrder() {
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap())
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)
            }
        }
    }

    @Test
    fun customPlaybackEvent() {
        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, defaultMetadataOverrides())

        // initialize and verify session
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario)
        // In the latest Conviva SDK, there is no problem with reporting app events before
        // a session is opened, as shown here.
        activityScenario.onActivity { activity: MainActivity ->
            convivaAnalyticsIntegration?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME)
            convivaAnalyticsIntegration?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)

            verifyOrder() {
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap())
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)
            }
        }

        loadSource(activityScenario, DEFAULT_DASH_VOD_SOURCE)

        activityScenario.onActivity { activity: MainActivity ->
            convivaAnalyticsIntegration?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME)
            convivaAnalyticsIntegration?.sendCustomPlaybackEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)

            verifyOrder() {
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap())
                ConvivaAnalytics.reportAppEvent(CUSTOM_EVENT_NAME, HashMap(CUSTOM_EVENT_ATTRIBUTES) as Map<String, Any>?)
            }
        }
    }

    @Test
    fun customErrorEvent() {
        // launch player with autoPlay enabled
        activityScenario = setupPlayerActivityForTest(autoPlay = true, defaultMetadataOverrides())

        // initialize and verify session
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario)

        // In the latest Conviva SDK, there is no problem with reporting app events before
        // a session is opened, as shown here.
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            convivaAnalyticsIntegration?.reportPlaybackDeficiency(
                CUSTOM_ERROR_MESSAGE,
                ConvivaSdkConstants.ErrorSeverity.WARNING,
                false
            )

            verify() {
                videoAnalyticsMock?.reportPlaybackError(
                        CUSTOM_ERROR_MESSAGE,
                        ConvivaSdkConstants.ErrorSeverity.WARNING
                )
            }

            convivaAnalyticsIntegration?.reportPlaybackDeficiency(
                CUSTOM_ERROR_MESSAGE,
                ConvivaSdkConstants.ErrorSeverity.FATAL,
                true
            )

            verify() {
                videoAnalyticsMock?.reportPlaybackError(
                        CUSTOM_ERROR_MESSAGE,
                        ConvivaSdkConstants.ErrorSeverity.FATAL
                )
            }
        }

        activityScenario = setupPlayerActivityForTest(autoPlay = true, defaultMetadataOverrides())

        // initialize and verify session  again
        initializeSession(activityScenario)
        verifySessionInitialization(activityScenario)

        // verify that custom error events are sent after Conviva session is initialized
        // but Conviva session is not ended when endSession argument is not passed as true
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            convivaAnalyticsIntegration?.reportPlaybackDeficiency(
                CUSTOM_ERROR_MESSAGE,
                ConvivaSdkConstants.ErrorSeverity.WARNING,
                false
            )
            verify(exactly = 1) {
                videoAnalyticsMock?.reportPlaybackError(
                    CUSTOM_ERROR_MESSAGE,
                    ConvivaSdkConstants.ErrorSeverity.WARNING
                )
            }
            verify(inverse = true) {
                videoAnalyticsMock?.reportPlaybackEnded()
            }
        }

        // verify that custom error events are sent after Conviva session is initialized
        // and Conviva session is also ended when endSession argument is passed as true
        activityScenario.onActivity { activity: MainActivity ->
            clearMocks(videoAnalyticsMock!!)
            convivaAnalyticsIntegration?.reportPlaybackDeficiency(
                    CUSTOM_ERROR_MESSAGE,
                    ConvivaSdkConstants.ErrorSeverity.FATAL,
                    true)
            verifyOrder() {
                videoAnalyticsMock?.reportPlaybackEnded()
            }
        }
    }
}
