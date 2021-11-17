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
import com.conviva.api.AndroidSystemInterfaceFactory;
import com.conviva.api.Client;
import com.conviva.api.ClientSettings;
import com.conviva.api.ContentMetadata;
import com.conviva.api.ConvivaException;
import com.conviva.api.SystemFactory;
import com.conviva.api.SystemSettings;
import com.conviva.api.player.PlayerStateManager;
import com.conviva.api.system.SystemInterface;

import java.util.HashMap;
import java.util.Map;

public class ConvivaAnalytics {

    private static final String TAG = "ConvivaAnalytics";

    private Client client = null;
    private Player bitmovinPlayer;
    private ContentMetadataBuilder contentMetadataBuilder = new ContentMetadataBuilder();
    private ConvivaConfig config;
    private int sessionId = Client.NO_SESSION_KEY;
    private PlayerStateManager playerStateManager;
    private ClientMeasureInterface clientMeasureInterface;
    private MetadataOverrides metadataOverrides;

    // Wrapper to extract bitmovinPlayer helper methods
    private BitmovinPlayerHelper playerHelper;

    // Helper
    private Boolean adStarted = false;

    public ConvivaAnalytics(Player player, String customerKey, Context context) {
        this(player, customerKey, context, new ConvivaConfig());
    }

    public ConvivaAnalytics(Player player,
                            String customerKey,
                            Context context,
                            ConvivaConfig config) {
        this(player, customerKey, context, new ConvivaConfig(), null);
    }

    public ConvivaAnalytics(Player player,
                            String customerKey,
                            Context context,
                            ConvivaConfig config,
                            Client client) {
        this.bitmovinPlayer = player;
        this.playerHelper = new BitmovinPlayerHelper(player);
        this.config = config;

        SystemInterface androidSystemInterface = AndroidSystemInterfaceFactory.buildSecure(context);
        if (androidSystemInterface.isInitialized()) {
            SystemSettings systemSettings = new SystemSettings();
            systemSettings.allowUncaughtExceptions = false;

            if (config.isDebugLoggingEnabled()) {
                systemSettings.logLevel = SystemSettings.LogLevel.DEBUG;
            }

            SystemFactory androidSystemFactory = new SystemFactory(androidSystemInterface, systemSettings);
            ClientSettings clientSettings = new ClientSettings(customerKey);

            if (config.getGatewayUrl() != null) {
                clientSettings.gatewayUrl = config.getGatewayUrl();
            }

            if (client != null) {
                this.client = client;
            } else {
                this.client = new Client(clientSettings, androidSystemFactory);
            }

            attachBitmovinEventListeners();
        }
    }

    private void ensureConvivaSessionIsCreatedAndInitialized() {
        if (!isSessionActive()) {
            internalInitializeSession();
        }
    }

    // region public methods
    public void sendCustomApplicationEvent(String name) {
        sendCustomApplicationEvent(name, new HashMap<String, Object>());
    }

    public void sendCustomApplicationEvent(String name, Map<String, Object> attributes) {
        try {
            client.sendCustomEvent(Client.NO_SESSION_KEY, name, attributes);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public void sendCustomPlaybackEvent(String name) {
        sendCustomPlaybackEvent(name, new HashMap<String, Object>());
    }

    public void sendCustomPlaybackEvent(String name, Map<String, Object> attributes) {
        if (!isSessionActive()) {
            Log.e(TAG, "Cannot send playback event, no active monitoring session");
            return;
        }
        try {
            client.sendCustomEvent(sessionId, name, attributes);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
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
        if (isSessionActive()) {
            return;
        }

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
     * Results in a no-opt if there is no active session.
     * <p>
     * Warning: The integration can only be validated without external session managing.
     * So when using this method we can no longer ensure that the session is managed at the correct time.
     */
    public void endSession() {
        if (!isSessionActive()) {
            return;
        }

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

        if (!this.isSessionActive()) {
            Log.i(TAG, "[ ConvivaAnalytics ] no active session; Don't propagate content metadata to conviva.");
            return;
        }

        this.createContentMetadata();
        this.updateSession();
    }

    /**
     * Sends a custom deficiency event during playback to Conviva's Player Insight. If no session is active it will NOT
     * create one.
     *
     * @param message Message which will be send to conviva
     * @param severity One of FATAL or WARNING
     */
    public void reportPlaybackDeficiency(String message, Client.ErrorSeverity severity) {
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
    public void reportPlaybackDeficiency(String message, Client.ErrorSeverity severity, Boolean endSession) {
        if (!isSessionActive()) {
            return;
        }

        try {
            this.client.reportError(this.sessionId, message, severity);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        if (endSession) {
            internalEndSession();
        }
    }

    /**
     * Puts the session state in a notMonitored state.
     */
    public void pauseTracking() {
        try {
            client.adStart(sessionId, Client.AdStream.SEPARATE, Client.AdPlayer.SEPARATE, Client.AdPosition.PREROLL);
            client.detachPlayer(sessionId);
            Log.d(TAG, "Tracking paused.");
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

    }

    /**
     * Puts the session state from a notMonitored state into the last one tracked.
     */
    public void resumeTracking() {
        try {
            client.attachPlayer(sessionId, playerStateManager);
            client.adEnd(sessionId);
            Log.d(TAG, "Tracking resumed.");
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }
    // endregion

    private void customEvent(Event event) {
        customEvent(event, new HashMap<String, Object>());
    }

    private void customEvent(Event event, Map<String, Object> attributes) {
        if (!isSessionActive()) {
            return;
        }

        String eventName = event.getClass().getSimpleName();
        sendCustomPlaybackEvent("on" + eventName, attributes);
    }

    // region Session handling
    private void setupPlayerStateManager() {
        try {
            playerStateManager = client.getPlayerStateManager();
            playerStateManager.setPlayerState(PlayerStateManager.PlayerState.STOPPED);
            playerStateManager.setPlayerType("Bitmovin Player Android");
            playerStateManager.setPlayerVersion(playerHelper.getSdkVersionString());
            clientMeasureInterface = new ClientMeasureInterface();
            playerStateManager.setClientMeasureInterface(clientMeasureInterface);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void internalInitializeSession() {
        try {
            createContentMetadata();
            sessionId = client.createSession(contentMetadataBuilder.build());
            setupPlayerStateManager();
            if (metadataOverrides != null) {
                updateContentMetadata(metadataOverrides);
            }
            Log.d(TAG, "[Player Event] Created SessionID - " + sessionId);
            client.attachPlayer(sessionId, playerStateManager);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void updateSession() {
        if (!isSessionActive()) {
            return;
        }
        this.buildDynamicContentMetadata();

        VideoQuality videoQuality = bitmovinPlayer.getPlaybackVideoData();
        if (videoQuality != null) {
            int bitrate = videoQuality.getBitrate() / 1000; // in kbps
            try {
                playerStateManager.setBitrateKbps(bitrate);
                playerStateManager.setVideoHeight(videoQuality.getHeight());
                playerStateManager.setVideoWidth(videoQuality.getWidth());
                clientMeasureInterface.setFrameRate(videoQuality.getFrameRate());
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        try {
            client.updateContentMetadata(sessionId, contentMetadataBuilder.build());
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
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
            contentMetadataBuilder.setStreamType(ContentMetadata.StreamType.LIVE);
        } else {
            contentMetadataBuilder.setStreamType(ContentMetadata.StreamType.VOD);
            contentMetadataBuilder.setDuration((int) bitmovinPlayer.getDuration());
        }

        contentMetadataBuilder.setStreamUrl(playerHelper.getStreamUrl());
    }

    private void internalEndSession() {
        if (!isSessionActive()) {
            return;
        }

        try {
            client.detachPlayer(sessionId);
            client.cleanupSession(sessionId);
            client.releasePlayerStateManager(playerStateManager);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } finally {
            sessionId = Client.NO_SESSION_KEY;
            playerStateManager = null;
            contentMetadataBuilder.reset();
            Log.e(TAG, "Session ended");
        }
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

    private synchronized void transitionState(PlayerStateManager.PlayerState state) {
        if (!isSessionActive()) {
            return;
        }

        try {
            Log.d(TAG, "Transitioning to :" + state.name());
            playerStateManager.setPlayerState(state);
        } catch (ConvivaException e) {
            Log.e(TAG, "Unable to transition state: " + e.getLocalizedMessage());
        }
    }

    // region Helper
    private boolean isSessionActive() {
        return sessionId != Client.NO_SESSION_KEY;
    }

    private void trackAdEnd() {
        if (!adStarted) {
            // Do not track adEnd if no ad is was shown (possible if an error occurred)
            return;
        }
        adStarted = false;

        try {
            client.adEnd(sessionId);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
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
            try {
                ensureConvivaSessionIsCreatedAndInitialized();

                String message = String.format("%s - %s", event.getCode(), event.getMessage());
                client.reportError(sessionId, message, Client.ErrorSeverity.FATAL);
                internalEndSession();
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private final EventListener<SourceEvent.Error> onSourceErrorListener = new EventListener<SourceEvent.Error>() {
        @Override
        public void onEvent(SourceEvent.Error event) {
            Log.d(TAG, "[Source Event] Error");
            try {
                ensureConvivaSessionIsCreatedAndInitialized();

                String message = String.format("%s - %s", event.getCode(), event.getMessage());
                client.reportError(sessionId, message, Client.ErrorSeverity.FATAL);
                internalEndSession();
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private final EventListener<PlayerEvent.Warning> onPlayerWarningListener = new EventListener<PlayerEvent.Warning>() {
        @Override
        public void onEvent(PlayerEvent.Warning warningEvent) {
            Log.d(TAG, "[Player Event] Warning");
            try {
                ensureConvivaSessionIsCreatedAndInitialized();

                String message = String.format("%s - %s", warningEvent.getCode(), warningEvent.getMessage());
                client.reportError(sessionId, message, Client.ErrorSeverity.WARNING);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private final EventListener<SourceEvent.Warning> onSourceWarningListener = new EventListener<SourceEvent.Warning>() {
        @Override
        public void onEvent(SourceEvent.Warning warningEvent) {
            Log.d(TAG, "[Source Event] Warning");
            try {
                ensureConvivaSessionIsCreatedAndInitialized();

                String message = String.format("%s - %s", warningEvent.getCode(), warningEvent.getMessage());
                client.reportError(sessionId, message, Client.ErrorSeverity.WARNING);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
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
            transitionState(PlayerStateManager.PlayerState.PLAYING);
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
                    transitionState(PlayerStateManager.PlayerState.PAUSED);
                }
            }, 100);
        }
    };

    private final EventListener<PlayerEvent.PlaybackFinished> onPlaybackFinishedListener = new EventListener<PlayerEvent.PlaybackFinished>() {
        @Override
        public void onEvent(PlayerEvent.PlaybackFinished playbackFinishedEvent) {
            Log.d(TAG, "[Player Event] PlaybackFinished");
            transitionState(PlayerStateManager.PlayerState.STOPPED);
            internalEndSession();
        }
    };

    private EventListener<PlayerEvent.StallStarted> onStallStartedListener = new EventListener<PlayerEvent.StallStarted>() {
        @Override
        public void onEvent(PlayerEvent.StallStarted stallStartedEvent) {
            Log.d(TAG, "[Player Event] StallStarted");
            transitionState(PlayerStateManager.PlayerState.BUFFERING);
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
                    PlayerStateManager.PlayerState state = PlayerStateManager.PlayerState.PLAYING;
                    if (bitmovinPlayer.isPaused()) {
                        state = PlayerStateManager.PlayerState.PAUSED;
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
        if (!isSessionActive()) {
            // Handle the case that the User seeks on the UI before play was triggered.
            // This also handles startTime feature. The same applies for onTimeShift.
            return;
        }
        Log.d(TAG, "Sending seek start event");
        try {
            playerStateManager.setPlayerSeekStart(seekTarget);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    };

    public void setSeekEnd() {
        if (!isSessionActive()) {
            // See comment in setSeekStart
            return;
        }
        Log.d(TAG, "Sending seek end event");
        try {
            playerStateManager.setPlayerSeekEnd();
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }
    // endregion

    // region Ad events
    private EventListener<PlayerEvent.AdStarted> onAdStartedListener = new EventListener<PlayerEvent.AdStarted>() {
        @Override
        public void onEvent(PlayerEvent.AdStarted adStartedEvent) {
            Log.d(TAG, "[Player Event] AdStarted");
            Client.AdPosition adPosition = AdEventUtil.parseAdPosition(adStartedEvent, bitmovinPlayer.getDuration());
            adStarted = true;

            try {
                client.adStart(sessionId, Client.AdStream.SEPARATE, Client.AdPlayer.CONTENT, adPosition);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
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
