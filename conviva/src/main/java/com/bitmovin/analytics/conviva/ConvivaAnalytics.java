package com.bitmovin.analytics.conviva;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.AdErrorEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
import com.bitmovin.player.api.event.data.AdSkippedEvent;
import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.api.event.data.BitmovinPlayerEvent;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.MutedEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlayEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.PlayingEvent;
import com.bitmovin.player.api.event.data.SeekEvent;
import com.bitmovin.player.api.event.data.SeekedEvent;
import com.bitmovin.player.api.event.data.SourceUnloadedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.TimeShiftEvent;
import com.bitmovin.player.api.event.data.TimeShiftedEvent;
import com.bitmovin.player.api.event.data.UnmutedEvent;
import com.bitmovin.player.api.event.data.VideoPlaybackQualityChangedEvent;
import com.bitmovin.player.api.event.data.WarningEvent;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnMutedListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlayListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnPlayingListener;
import com.bitmovin.player.api.event.listener.OnSeekListener;
import com.bitmovin.player.api.event.listener.OnSeekedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnTimeShiftListener;
import com.bitmovin.player.api.event.listener.OnTimeShiftedListener;
import com.bitmovin.player.api.event.listener.OnUnmutedListener;
import com.bitmovin.player.api.event.listener.OnVideoPlaybackQualityChangedListener;
import com.bitmovin.player.api.event.listener.OnWarningListener;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.config.quality.VideoQuality;
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
    private BitmovinPlayer bitmovinPlayer;
    private ContentMetadataBuilder contentMetadataBuilder = new ContentMetadataBuilder();
    private ConvivaConfiguration config;
    private int sessionId = Client.NO_SESSION_KEY;
    private PlayerStateManager playerStateManager;
    private ClientMeasureInterface clientMeasureInterface;
    private MetadataOverrides metadataOverrides;

    // Wrapper to extract bitmovinPlayer helper methods
    private BitmovinPlayerHelper playerHelper;

    // Helper
    private Boolean adStarted = false;

    public ConvivaAnalytics(BitmovinPlayer player, String customerKey, Context context) {
        this(player, customerKey, context, new ConvivaConfiguration());
    }

    public ConvivaAnalytics(BitmovinPlayer player,
                            String customerKey,
                            Context context,
                            ConvivaConfiguration config) {
        this(player, customerKey, context, new ConvivaConfiguration(), null);
    }

    public ConvivaAnalytics(BitmovinPlayer player,
                            String customerKey,
                            Context context,
                            ConvivaConfiguration config,
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

        if ((bitmovinPlayer.getConfig().getSourceItem() == null
                || bitmovinPlayer.getConfig().getSourceItem().getTitle() == null)
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

    private void customEvent(BitmovinPlayerEvent event) {
        customEvent(event, new HashMap<String, Object>());
    }

    private void customEvent(BitmovinPlayerEvent event, Map<String, Object> attributes) {
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
        SourceItem sourceItem = bitmovinPlayer.getConfig().getSourceItem();
        if (sourceItem != null) {
            contentMetadataBuilder.setAssetName(sourceItem.getTitle());
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
        bitmovinPlayer.addEventListener(onSourceUnloadedListener);
        bitmovinPlayer.addEventListener(onErrorListener);
        bitmovinPlayer.addEventListener(onWarningListener);

        bitmovinPlayer.addEventListener(onMutedListener);
        bitmovinPlayer.addEventListener(onUnmutedListener);

        // Playback state events
        bitmovinPlayer.addEventListener(onPlayListener);
        bitmovinPlayer.addEventListener(onPlayingListener);
        bitmovinPlayer.addEventListener(onPausedListener);
        bitmovinPlayer.addEventListener(onStallEndedListener);
        bitmovinPlayer.addEventListener(onStallStartedListener);
        bitmovinPlayer.addEventListener(onPlaybackFinishedListener);

        // Seek events
        bitmovinPlayer.addEventListener(onSeekedListener);
        bitmovinPlayer.addEventListener(onSeekListener);

        // Timeshift events
        bitmovinPlayer.addEventListener(onTimeShiftListener);
        bitmovinPlayer.addEventListener(onTimeShiftedListener);

        // Ad events
        bitmovinPlayer.addEventListener(onAdStartedListener);
        bitmovinPlayer.addEventListener(onAdFinishedListener);
        bitmovinPlayer.addEventListener(onAdSkippedListener);
        bitmovinPlayer.addEventListener(onAdErrorListener);

        bitmovinPlayer.addEventListener(onVideoPlaybackQualityChangedListener);
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
    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            // The default SDK error handling is that it triggers the onSourceUnloaded before the onError event.
            // To track errors on Conviva we need to delay the onSourceUnloaded to ensure the onError event is
            // called first.
            // TODO: remove this once the event order is fixed on the Android SDK.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[Player Event] OnSourceUnloaded");
                    internalEndSession();
                }
            }, 100);
        }
    };

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(ErrorEvent errorEvent) {
            Log.d(TAG, "[Player Event] OnError");
            try {
                ensureConvivaSessionIsCreatedAndInitialized();

                String message = String.format("%s - %s", errorEvent.getCode(), errorEvent.getMessage());
                client.reportError(sessionId, message, Client.ErrorSeverity.FATAL);
                internalEndSession();
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private OnWarningListener onWarningListener = new OnWarningListener() {
        @Override
        public void onWarning(WarningEvent warningEvent) {
            Log.d(TAG, "[Player Event] OnWarning");
            try {
                ensureConvivaSessionIsCreatedAndInitialized();

                String message = String.format("%s - %s", warningEvent.getCode(), warningEvent.getMessage());
                client.reportError(sessionId, message, Client.ErrorSeverity.WARNING);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private OnMutedListener onMutedListener = new OnMutedListener() {
        @Override
        public void onMuted(MutedEvent mutedEvent) {
            Log.d(TAG, "[Player Event] OnMuted");
            customEvent(mutedEvent);
        }
    };

    private OnUnmutedListener onUnmutedListener = new OnUnmutedListener() {
        @Override
        public void onUnmuted(UnmutedEvent unmutedEvent) {
            Log.d(TAG, "[Player Event] OnUnmuted");
            customEvent(unmutedEvent);
        }
    };

    // region Playback state events
    private OnPlayListener onPlayListener = new OnPlayListener() {
        @Override
        public void onPlay(PlayEvent playEvent) {
            Log.d(TAG, "[Player Event] OnPlay");
            ensureConvivaSessionIsCreatedAndInitialized();
            updateSession();
        }
    };

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            Log.d(TAG, "[Player Event] OnPlaying");
            contentMetadataBuilder.setPlaybackStarted(true);
            transitionState(PlayerStateManager.PlayerState.PLAYING);
        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            // The default SDK handling is that it triggers the onPaused before the
            // onError event in case of no internet connectivity. (No onPaused should be triggered)
            // To ensure that no playback state change will be reported we need to delay the
            // onPaused event.
            // TODO: remove this once the event order is fixed on the Android SDK.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[Player Event] OnPaused");
                    transitionState(PlayerStateManager.PlayerState.PAUSED);
                }
            }, 100);
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            Log.d(TAG, "[Player Event] OnPlaybackFinished");
            transitionState(PlayerStateManager.PlayerState.STOPPED);
            internalEndSession();
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            Log.d(TAG, "[Player Event] OnStallStarted");
            transitionState(PlayerStateManager.PlayerState.BUFFERING);
        }
    };

    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            // The default SDK error handling is that it triggers the onStallEnded before the
            // onError event in case of no internet connectivity.
            // To track errors on Conviva we need to delay the onStallEnded to ensure no
            // playback state change will be reported.
            // TODO: remove this once the event order is fixed on the Android SDK.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "[Player Event] OnStallEnded");
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
    private OnSeekListener onSeekListener = new OnSeekListener() {
        @Override
        public void onSeek(SeekEvent seekEvent) {
            Log.d(TAG, "[Player Event] OnSeek");
            setSeekStart((int) seekEvent.getSeekTarget() * 1000);
        }
    };

    private OnSeekedListener onSeekedListener = new OnSeekedListener() {
        @Override
        public void onSeeked(SeekedEvent seekedEvent) {
            Log.d(TAG, "[Player Event] OnSeeked");
            setSeekEnd();
        }
    };

    private OnTimeShiftListener onTimeShiftListener = new OnTimeShiftListener() {
        @Override
        public void onTimeShift(TimeShiftEvent timeShiftEvent) {
            Log.d(TAG, "[Player Event] OnTimeShift");
            // According to conviva it is valid to pass -1 for seeking in live streams
            setSeekStart(-1);
        }
    };

    private OnTimeShiftedListener onTimeShiftedListener = new OnTimeShiftedListener() {
        @Override
        public void onTimeShifted(TimeShiftedEvent timeShiftedEvent) {
            Log.d(TAG, "[Player Event] OnTimeShifted");
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
    private OnAdStartedListener onAdStartedListener = new OnAdStartedListener() {
        @Override
        public void onAdStarted(AdStartedEvent adStartedEvent) {
            Client.AdPosition adPosition = AdEventUtil.parseAdPosition(adStartedEvent, bitmovinPlayer.getDuration());
            adStarted = true;

            try {
                client.adStart(sessionId, Client.AdStream.SEPARATE, Client.AdPlayer.CONTENT, adPosition);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private OnAdFinishedListener onAdFinishedListener = new OnAdFinishedListener() {
        @Override
        public void onAdFinished(AdFinishedEvent adFinishedEvent) {
            trackAdEnd();
        }
    };

    private OnAdSkippedListener onAdSkippedListener = new OnAdSkippedListener() {
        @Override
        public void onAdSkipped(AdSkippedEvent adSkippedEvent) {
            customEvent(adSkippedEvent);
            trackAdEnd();
        }
    };

    private OnAdErrorListener onAdErrorListener = new OnAdErrorListener() {
        @Override
        public void onAdError(AdErrorEvent adErrorEvent) {
            customEvent(adErrorEvent);
            trackAdEnd();
        }
    };
    // endregion

    private OnVideoPlaybackQualityChangedListener onVideoPlaybackQualityChangedListener = new OnVideoPlaybackQualityChangedListener() {
        @Override
        public void onVideoPlaybackQualityChanged(VideoPlaybackQualityChangedEvent videoPlaybackQualityChangedEvent) {
            Log.d(TAG, "[Player Event] OnVideoPlaybackQualityChanged");
            updateSession();
        }
    };
    // endregion
}