package com.bitmovin.analytics.conviva;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.event.Event;
import com.bitmovin.player.api.event.EventListener;
import com.bitmovin.player.api.event.PlayerEvent;
import com.bitmovin.player.api.event.SourceEvent;
import com.bitmovin.player.api.media.video.quality.VideoQuality;
import com.bitmovin.player.api.source.Source;
import com.bitmovin.player.api.source.SourceConfig;
import com.conviva.api.ConvivaConstants;
import com.conviva.sdk.ConvivaAdAnalytics;
import com.conviva.sdk.ConvivaAnalytics;
import com.conviva.sdk.ConvivaSdkConstants;
import com.conviva.sdk.ConvivaVideoAnalytics;

import java.util.HashMap;
import java.util.Map;

public class ConvivaAnalyticsIntegration {

    private static final String TAG = "ConvivaAnalyticsInt";

    private Player bitmovinPlayer;
    private ContentMetadataBuilder contentMetadataBuilder = new ContentMetadataBuilder();
    private ConvivaConfig config;
    private ConvivaVideoAnalytics convivaVideoAnalytics;
    private ConvivaAdAnalytics convivaAdAnalytics;
    private MetadataOverrides metadataOverrides;

    // Wrapper to extract bitmovinPlayer helper methods
    private BitmovinPlayerHelper playerHelper;

    // Helper
    private Boolean adStarted = false;
    private Boolean isSessionActive = false;
    private Boolean isBumper = false;

    public ConvivaAnalyticsIntegration(Player player, String customerKey, Context context) {
        this(player, customerKey, context, new ConvivaConfig());
    }

    public ConvivaAnalyticsIntegration(Player player,
                                       String customerKey,
                                       Context context,
                                       ConvivaConfig config) {
        this.bitmovinPlayer = player;
        this.playerHelper = new BitmovinPlayerHelper(player);
        this.config = config;

        if(BuildConfig.DEBUG) {
            Map<String, Object> settings = new HashMap<String, Object>();
            if (config.getGatewayUrl() != null) {
                settings.put(ConvivaSdkConstants.GATEWAY_URL, config.getGatewayUrl());
            }
            if (config.isDebugLoggingEnabled()) {
                settings.put(ConvivaSdkConstants.LOG_LEVEL, ConvivaSdkConstants.LogLevel.DEBUG);
            }
            ConvivaAnalytics.init(context, customerKey, settings);
        } else {
            ConvivaAnalytics.init(context, customerKey);
        }

        convivaVideoAnalytics = ConvivaAnalytics.buildVideoAnalytics(context);
        convivaAdAnalytics = ConvivaAnalytics.buildAdAnalytics(context, convivaVideoAnalytics);

        attachBitmovinEventListeners();
    }

    // region public methods
    public void sendCustomApplicationEvent(String name) {
        sendCustomApplicationEvent(name, new HashMap<String, Object>());
    }

    public void sendCustomApplicationEvent(String name, Map<String, Object> attributes) {
        Log.d(TAG, "Will send custom application event: " + name + " " + attributes.toString());
        ConvivaAnalytics.reportAppEvent(name, attributes);
    }

    public void sendCustomPlaybackEvent(String name) {
        sendCustomPlaybackEvent(name, new HashMap<String, Object>());
    }

    public void sendCustomPlaybackEvent(String name, Map<String, Object> attributes) {
        Log.d(TAG, "Will report app event: " + name + " " + attributes.toString());
        ConvivaAnalytics.reportAppEvent(name, attributes);
    }

    /**
     * Initializes a new conviva tracking session.
     * <p>
     * Warning: The integration can only be validated without external session managing. So when using this method we can
     * no longer ensure that the session is managed at the correct time. Additional: Since some metadata attributes
     * relies on the players source we can't ensure that all metadata attributes are present at session creation.
     * Therefore it could be that there will be a 'ContentMetadata created late' issue after conviva validation.
     * <p>
     * If no source was loaded this method will throw an error.
     */
    public void initializeSession() throws ConvivaAnalyticsException {
        if ((bitmovinPlayer.getSource() == null ||
                bitmovinPlayer.getSource().getConfig() == null
                || bitmovinPlayer.getSource().getConfig().getTitle() == null)
                && this.contentMetadataBuilder.getAssetName() == null) {
            throw new ConvivaAnalyticsException(
                    "AssetName is missing. Load player source (with Title) first or set assetName via updateContentMetadata"
            );
        }
        internalInitializeSession();
    }

    /**
     * Ends the current conviva tracking session.
     * If there is no active session, the only thing that will happen is resetting the content metadata.
     * <p>
     * Warning: The integration can only be validated without external session managing.
     * So when using this method we can no longer ensure that the session is managed at the correct time.
     */
    public void endSession() {
        internalEndSession();
    }

    /**
     * Will update the contentMetadata which are tracked with conviva.
     * <p>
     * If there is an active session only permitted values will be updated and propagated immediately.
     * If there is no active session the values will be set on session creation.
     * <p>
     * Attributes set via this method will override automatic tracked once.
     *
     * @param metadataOverrides MetadataOverrides attributes which will be used to track to conviva.
     * @see ContentMetadataBuilder for more information about permitted attributes
     */
    public void updateContentMetadata(MetadataOverrides metadataOverrides) {
        this.contentMetadataBuilder.setOverrides(metadataOverrides);
        this.metadataOverrides = metadataOverrides;
        this.createContentMetadata();
        this.updateSession();
    }

    public void release() {
        convivaVideoAnalytics.release();
        ConvivaAnalytics.release();
    }

    /**
     * Sends a custom deficiency event during playback to Conviva's Player Insight. If no session is active it will NOT
     * create one.
     *
     * @param message Message which will be send to conviva
     * @param severity One of FATAL or WARNING
     */
    public void reportPlaybackDeficiency(String message, ConvivaSdkConstants.ErrorSeverity severity) {
        reportPlaybackDeficiency(message, severity, true);
    }

    /**
     * Sends a custom deficiency event during playback to Conviva's Player Insight. If no session is active it will NOT
     * create one.
     *
     * @param message Message which will be send to conviva
     * @param severity One of FATAL or WARNING
     * @param endSession Boolean flag if session should be closed after reporting the deficiency
     */
    public void reportPlaybackDeficiency(String message, ConvivaSdkConstants.ErrorSeverity severity, Boolean endSession) {
        Log.d(TAG, "Will report playback deficiency: " + message + ",  " + severity.toString());
        if (isSessionActive) {
            convivaVideoAnalytics.reportPlaybackError(message, severity);
        }
        if (endSession) {
            internalEndSession();
        }
    }

    /**
     * Puts the session state in a notMonitored state.
     */
    public void pauseTracking(Boolean _isBumper) {
        isBumper = _isBumper;
        String event = isBumper ? ConvivaSdkConstants.Events.BUMPER_VIDEO_STARTED.toString() : ConvivaSdkConstants.Events.USER_WAIT_STARTED.toString();
        convivaVideoAnalytics.reportPlaybackEvent(event);
        Log.d(TAG, "Tracking paused.");
    }

    /**
     * Puts the session state from a notMonitored state into the last one tracked.
     */
    public void resumeTracking() {
        String event = isBumper ? ConvivaSdkConstants.Events.BUMPER_VIDEO_ENDED.toString() : ConvivaSdkConstants.Events.USER_WAIT_ENDED.toString();
        convivaVideoAnalytics.reportPlaybackEvent(event);
        Log.d(TAG, "Tracking resumed.");
    }

    /**
     * This should be called when the app is resumed
     */
    public void reportAppForegrounded() {
        Log.d(TAG, "appForegrounded");
        ConvivaAnalytics.reportAppForegrounded();
        if(!isSessionActive) {
            isSessionActive = true;
        }
    }

    /**
     * This should be called when the app is paused
     */
    public void reportAppBackgrounded() {
        Log.d(TAG, "appBackgrounded");
        if(isSessionActive) {
            ConvivaAnalytics.reportAppBackgrounded();
            isSessionActive = false;
        }

    }

    // endregion

    private void ensureConvivaSessionIsCreatedAndInitialized() {
        if (!isSessionActive) {
            internalInitializeSession();
        }
    }

    private void customEvent(Event event) {
        customEvent(event, new HashMap<String, Object>());
    }

    private void customEvent(Event event, Map<String, Object> attributes) {
        String eventName = event.getClass().getSimpleName();
        sendCustomPlaybackEvent("on" + eventName, attributes);
    }

    // region Session handling
    private void setupPlayerStateManager() {
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, ConvivaSdkConstants.PlayerState.STOPPED);
        Map<String, Object> playerInfo = new HashMap<String, Object>();
        playerInfo.put(ConvivaSdkConstants.FRAMEWORK_NAME,"Bitmovin Player Android");
        playerInfo.put(ConvivaSdkConstants.FRAMEWORK_VERSION, playerHelper.getSdkVersionString());
        convivaVideoAnalytics.setPlayerInfo(playerInfo);
    }

    private void internalInitializeSession() {
        if(isSessionActive) {
            return;
        }
        Log.d(TAG, "internalInitializeSession");
        createContentMetadata();
        convivaVideoAnalytics.reportPlaybackRequested(contentMetadataBuilder.build());
        setupPlayerStateManager();
        if (metadataOverrides != null) {
            updateContentMetadata(metadataOverrides);
        }
        isSessionActive = true;
    }

    private void updateSession() {
        this.buildDynamicContentMetadata();

        VideoQuality videoQuality = bitmovinPlayer.getPlaybackVideoData();
        if (videoQuality != null) {
            int bitrate = videoQuality.getBitrate() / 1000; // in kbps
                convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, videoQuality.getHeight(), videoQuality.getWidth());
                convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, bitrate);
                convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, Math.round(videoQuality.getFrameRate()));
        }

        if(isSessionActive) {
            convivaVideoAnalytics.setContentInfo(contentMetadataBuilder.build());
        }
    }

    private void createContentMetadata() {
        Source source = bitmovinPlayer.getSource();
        if (source != null) {
            SourceConfig sourceConfig = source.getConfig();
            if (sourceConfig != null) {
                contentMetadataBuilder.setAssetName(sourceConfig.getTitle());
            }
        }
        this.buildDynamicContentMetadata();
    }

    private void buildDynamicContentMetadata() {
        // Build custom tags here, though this is static metadata but
        // streamType could be missing at time of session initialization
        // as source information could be unavailable at that time
        Map<String, String> customInternTags = new HashMap<>();
        customInternTags.put("streamType", playerHelper.getStreamType());
        customInternTags.put("integrationVersion", BuildConfig.VERSION_NAME);
        contentMetadataBuilder.setCustom(customInternTags);

        if (bitmovinPlayer.isLive()) {
            contentMetadataBuilder.setStreamType(ConvivaSdkConstants.StreamType.LIVE);
        } else {
            contentMetadataBuilder.setStreamType(ConvivaSdkConstants.StreamType.VOD);
            contentMetadataBuilder.setDuration((int) bitmovinPlayer.getDuration());
        }

        contentMetadataBuilder.setStreamUrl(playerHelper.getStreamUrl());
    }

    private void internalEndSession() {
        contentMetadataBuilder.reset();
        if(!isSessionActive) {
            return;
        }
        convivaVideoAnalytics.reportPlaybackEnded();
        Log.d(TAG, "Session ended");
        isSessionActive = false;
    }
    // endregion

    private void attachBitmovinEventListeners() {
        bitmovinPlayer.on(SourceEvent.Unloaded.class, onSourceUnloadedListener);
        bitmovinPlayer.on(PlayerEvent.Error.class, onPlayerErrorListener);
        bitmovinPlayer.on(SourceEvent.Error.class, onSourceErrorListener);
        bitmovinPlayer.on(PlayerEvent.Warning.class, onPlayerWarningListener);
        bitmovinPlayer.on(SourceEvent.Warning.class, onSourceWarningListener);

        bitmovinPlayer.on(PlayerEvent.Muted.class, onMutedListener);
        bitmovinPlayer.on(PlayerEvent.Unmuted.class, onUnmutedListener);

        // Playback state events
        bitmovinPlayer.on(PlayerEvent.Play.class, onPlayListener);
        bitmovinPlayer.on(PlayerEvent.Playing.class, onPlayingListener);
        bitmovinPlayer.on(PlayerEvent.Paused.class, onPausedListener);
        bitmovinPlayer.on(PlayerEvent.StallEnded.class, onStallEndedListener);
        bitmovinPlayer.on(PlayerEvent.StallStarted.class, onStallStartedListener);
        bitmovinPlayer.on(PlayerEvent.PlaybackFinished.class, onPlaybackFinishedListener);

        // Seek events
        bitmovinPlayer.on(PlayerEvent.Seeked.class, onSeekedListener);
        bitmovinPlayer.on(PlayerEvent.Seek.class, onSeekListener);

        // Timeshift events
        bitmovinPlayer.on(PlayerEvent.TimeShift.class, onTimeShiftListener);
        bitmovinPlayer.on(PlayerEvent.TimeShifted.class, onTimeShiftedListener);

        // Ad events
        bitmovinPlayer.on(PlayerEvent.AdStarted.class, onAdStartedListener);
        bitmovinPlayer.on(PlayerEvent.AdFinished.class, onAdFinishedListener);
        bitmovinPlayer.on(PlayerEvent.AdSkipped.class, onAdSkippedListener);
        bitmovinPlayer.on(PlayerEvent.AdError.class, onAdErrorListener);

        bitmovinPlayer.on(PlayerEvent.VideoPlaybackQualityChanged.class, onVideoPlaybackQualityChangedListener);
    }

    private synchronized void transitionState(ConvivaSdkConstants.PlayerState state) {
        Log.d(TAG, "Transitioning to :" + state.name());
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, state);
    }

    // region Helper

    private void trackAdEnd() {
        if (!adStarted) {
            // Do not track adEnd if no ad is was shown (possible if an error occurred)
            return;
        }
        adStarted = false;

        Log.d(TAG, "Report ad break ended");
        convivaVideoAnalytics.reportAdBreakEnded();
    }
    // endregion

    // region Listeners
    private final EventListener<SourceEvent.Unloaded> onSourceUnloadedListener = new EventListener<SourceEvent.Unloaded>() {
        @Override
        public void onEvent(SourceEvent.Unloaded event) {
        // The default SDK error handling is that it triggers the onSourceUnloaded before the onError event.
        // To track errors on Conviva we need to delay the onSourceUnloaded to ensure the onError event is
        // called first.
        // TODO: remove this once the event order is fixed on the Android SDK.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "[Player Event] SourceUnloaded");
                internalEndSession();
            }
        }, 100);
        }
    };

    private final EventListener<PlayerEvent.Error> onPlayerErrorListener = new EventListener<PlayerEvent.Error>() {
        @Override
        public void onEvent(PlayerEvent.Error event) {
            Log.d(TAG, "[Player Event] Error");
            String message = String.format("%s - %s", event.getCode(), event.getMessage());
            convivaVideoAnalytics.reportPlaybackError(message, ConvivaSdkConstants.ErrorSeverity.FATAL);
            internalEndSession();
        }
    };

    private final EventListener<SourceEvent.Error> onSourceErrorListener = new EventListener<SourceEvent.Error>() {
        @Override
        public void onEvent(SourceEvent.Error event) {
            Log.d(TAG, "[Source Event] Error");
            String message = String.format("%s - %s", event.getCode(), event.getMessage());
            convivaVideoAnalytics.reportPlaybackError(message, ConvivaSdkConstants.ErrorSeverity.FATAL);
            internalEndSession();
        }
    };

    private final EventListener<PlayerEvent.Warning> onPlayerWarningListener = new EventListener<PlayerEvent.Warning>() {
        @Override
        public void onEvent(PlayerEvent.Warning warningEvent) {
            Log.d(TAG, "[Player Event] Warning");
            String message = String.format("%s - %s", warningEvent.getCode(), warningEvent.getMessage());
            convivaVideoAnalytics.reportPlaybackError(message, ConvivaSdkConstants.ErrorSeverity.WARNING);
        }
    };

    private final EventListener<SourceEvent.Warning> onSourceWarningListener = new EventListener<SourceEvent.Warning>() {
        @Override
        public void onEvent(SourceEvent.Warning warningEvent) {
            Log.d(TAG, "[Source Event] Warning");
            String message = String.format("%s - %s", warningEvent.getCode(), warningEvent.getMessage());
            convivaVideoAnalytics.reportPlaybackError(message, ConvivaSdkConstants.ErrorSeverity.WARNING);
        }
    };

    private final EventListener<PlayerEvent.Muted> onMutedListener = new EventListener<PlayerEvent.Muted>() {
        @Override
        public void onEvent(PlayerEvent.Muted event) {
            Log.d(TAG, "[Player Event] Muted");
            customEvent(event);
        }
    };

    private final EventListener<PlayerEvent.Unmuted> onUnmutedListener = new EventListener<PlayerEvent.Unmuted>() {
        @Override
        public void onEvent(PlayerEvent.Unmuted event) {
            Log.d(TAG, "[Player Event] Unmuted");
            customEvent(event);
        }
    };

    // region Playback state events
    private final EventListener<PlayerEvent.Play> onPlayListener = new EventListener<PlayerEvent.Play>() {
        @Override
        public void onEvent(PlayerEvent.Play playEvent) {
            Log.d(TAG, "[Player Event] Play");
            ensureConvivaSessionIsCreatedAndInitialized();
            updateSession();
        }
    };

    private final EventListener<PlayerEvent.Playing> onPlayingListener = new EventListener<PlayerEvent.Playing>() {
        @Override
        public void onEvent(PlayerEvent.Playing playingEvent) {
            Log.d(TAG, "[Player Event] Playing");
            contentMetadataBuilder.setPlaybackStarted(true);
            transitionState(ConvivaSdkConstants.PlayerState.PLAYING);
        }
    };

    private EventListener<PlayerEvent.Paused> onPausedListener = new EventListener<PlayerEvent.Paused>() {
        @Override
        public void onEvent(PlayerEvent.Paused pausedEvent) {
            // The default SDK handling is that it triggers the onPaused before the
            // onError event in case of no internet connectivity. (No onPaused should be triggered)
            // To ensure that no playback state change will be reported we need to delay the
            // onPaused event.
            // TODO: remove this once the event order is fixed on the Android SDK.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[Player Event] Paused");
                    transitionState(ConvivaSdkConstants.PlayerState.PAUSED);
                }
            }, 100);
        }
    };

    private final EventListener<PlayerEvent.PlaybackFinished> onPlaybackFinishedListener = new EventListener<PlayerEvent.PlaybackFinished>() {
        @Override
        public void onEvent(PlayerEvent.PlaybackFinished playbackFinishedEvent) {
            Log.d(TAG, "[Player Event] PlaybackFinished");
            transitionState(ConvivaSdkConstants.PlayerState.STOPPED);
            internalEndSession();
        }
    };

    private EventListener<PlayerEvent.StallStarted> onStallStartedListener = new EventListener<PlayerEvent.StallStarted>() {
        @Override
        public void onEvent(PlayerEvent.StallStarted stallStartedEvent) {
            Log.d(TAG, "[Player Event] StallStarted");
            transitionState(ConvivaSdkConstants.PlayerState.BUFFERING);
        }
    };

    private EventListener<PlayerEvent.StallEnded> onStallEndedListener = new EventListener<PlayerEvent.StallEnded>() {
        @Override
        public void onEvent(PlayerEvent.StallEnded stallEndedEvent) {
            // The default SDK error handling is that it triggers the onStallEnded before the
            // onError event in case of no internet connectivity.
            // To track errors on Conviva we need to delay the onStallEnded to ensure no
            // playback state change will be reported.
            // TODO: remove this once the event order is fixed on the Android SDK.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[Player Event] StallEnded");
                    ConvivaSdkConstants.PlayerState state = ConvivaSdkConstants.PlayerState.PLAYING;
                    if (bitmovinPlayer.isPaused()) {
                        state = ConvivaSdkConstants.PlayerState.PAUSED;
                    }
                    transitionState(state);
                }
            }, 100);
        }
    };
    // endregion

    // region Seek and Timeshift events
    private EventListener<PlayerEvent.Seek> onSeekListener = new EventListener<PlayerEvent.Seek>() {
        @Override
        public void onEvent(PlayerEvent.Seek seekEvent) {
            Log.d(TAG, "[Player Event] Seek");
            setSeekStart((int) seekEvent.getTo().getTime() * 1000);
        }
    };

    private EventListener<PlayerEvent.Seeked> onSeekedListener = new EventListener<PlayerEvent.Seeked>() {
        @Override
        public void onEvent(PlayerEvent.Seeked seekedEvent) {
            Log.d(TAG, "[Player Event] Seeked");
            setSeekEnd();
        }
    };

    private EventListener<PlayerEvent.TimeShift> onTimeShiftListener = new EventListener<PlayerEvent.TimeShift>() {
        @Override
        public void onEvent(PlayerEvent.TimeShift timeShiftEvent) {
            Log.d(TAG, "[Player Event] TimeShift");
            // According to conviva it is valid to pass -1 for seeking in live streams
            setSeekStart(-1);
        }
    };

    private EventListener<PlayerEvent.TimeShifted> onTimeShiftedListener = new EventListener<PlayerEvent.TimeShifted>() {
        @Override
        public void onEvent(PlayerEvent.TimeShifted timeShiftedEvent) {
            Log.d(TAG, "[Player Event] TimeShifted");
            setSeekEnd();
        }
    };

    private void setSeekStart(int seekTarget) {
        Log.d(TAG, "Sending seek start event");
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED, seekTarget);
    };

    public void setSeekEnd() {
        Log.d(TAG, "Sending seek end event");
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED);
    }
    // endregion

    // region Ad events
    private EventListener<PlayerEvent.AdStarted> onAdStartedListener = new EventListener<PlayerEvent.AdStarted>() {
        @Override
        public void onEvent(PlayerEvent.AdStarted adStartedEvent) {
            Log.d(TAG, "[Player Event] AdStarted");
            adStarted = true;
            convivaVideoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.SERVER_SIDE);
        }
    };

    private EventListener<PlayerEvent.AdFinished> onAdFinishedListener = new EventListener<PlayerEvent.AdFinished>() {
        @Override
        public void onEvent(PlayerEvent.AdFinished adFinishedEvent) {
            Log.d(TAG, "[Player Event] AdFinished");
            trackAdEnd();
        }
    };

    private EventListener<PlayerEvent.AdSkipped> onAdSkippedListener = new EventListener<PlayerEvent.AdSkipped>() {
        @Override
        public void onEvent(PlayerEvent.AdSkipped adSkippedEvent) {
            Log.d(TAG, "[Player Event] AdSkipped");
            customEvent(adSkippedEvent);
            trackAdEnd();
        }
    };

    private EventListener<PlayerEvent.AdError> onAdErrorListener = new EventListener<PlayerEvent.AdError>() {
        @Override
        public void onEvent(PlayerEvent.AdError adErrorEvent) {
            Log.d(TAG, "[Player Event] AdError");
            customEvent(adErrorEvent);
            trackAdEnd();
        }
    };
    // endregion

    private EventListener<PlayerEvent.VideoPlaybackQualityChanged> onVideoPlaybackQualityChangedListener = new EventListener<PlayerEvent.VideoPlaybackQualityChanged>() {
        @Override
        public void onEvent(PlayerEvent.VideoPlaybackQualityChanged videoPlaybackQualityChangedEvent) {
            Log.d(TAG, "[Player Event] VideoPlaybackQualityChanged");
            updateSession();
        }
    };
    // endregion
}
