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
import com.conviva.api.player.PlayerStateManager;
import com.conviva.sdk.ConvivaExperienceAnalytics;
import com.conviva.sdk.ConvivaSdkConstants;
import com.conviva.sdk.ConvivaVideoAnalytics;

import java.util.HashMap;
import java.util.Map;

public class ConvivaAnalytics {

    private static final String TAG = "ConvivaAnalytics";

    private BitmovinPlayer bitmovinPlayer;
    private ContentMetadataBuilder contentMetadataBuilder = new ContentMetadataBuilder();
    private ConvivaConfiguration config;
    private Context context;
    private ConvivaVideoAnalytics convivaVideoAnalytics;
    private MetadataOverrides metadataOverrides;
    private boolean activeSession = false;

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
        this.bitmovinPlayer = player;
        this.playerHelper = new BitmovinPlayerHelper(player);
        this.context = context;
        this.config = config;

        Map<String, Object> convivaSettings = new HashMap<>();
        if (config.isDebugLoggingEnabled()) {
            convivaSettings.put(ConvivaSdkConstants.LOG_LEVEL, ConvivaSdkConstants.LogLevel.DEBUG);
        }
        if (config.getGatewayUrl() != null) {
            convivaSettings.put(ConvivaSdkConstants.GATEWAY_URL, config.getGatewayUrl());
        }

        com.conviva.sdk.ConvivaAnalytics.init(context, customerKey, convivaSettings);

        attachBitmovinEventListeners();
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
        com.conviva.sdk.ConvivaAnalytics.reportAppEvent(name, attributes);
    }

    public void sendCustomPlaybackEvent(String name) {
        sendCustomPlaybackEvent(name, new HashMap<String, Object>());
    }

    public void sendCustomPlaybackEvent(String name, Map<String, Object> attributes) {
        if (!isSessionActive()) {
            Log.e(TAG, "Cannot send playback event, no active monitoring session");
            return;
        }
        com.conviva.sdk.ConvivaAnalytics.reportAppEvent(name, attributes);
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
     * @param message  Message which will be send to conviva
     * @param severity One of FATAL or WARNING
     */
    public void reportPlaybackDeficiency(String message, ConvivaSdkConstants.ErrorSeverity severity) {
        reportPlaybackDeficiency(message, severity, true);
    }

    /**
     * Sends a custom deficiency event during playback to Conviva's Player Insight. If no session is active it will NOT
     * create one.
     *
     * @param message    Message which will be send to conviva
     * @param severity   One of FATAL or WARNING
     * @param endSession Boolean flag if session should be closed after reporting the deficiency
     */
    public void reportPlaybackDeficiency(String message, ConvivaSdkConstants.ErrorSeverity severity, Boolean endSession) {
        if (!isSessionActive()) {
            return;
        }

        if (convivaVideoAnalytics != null) {
            convivaVideoAnalytics.reportPlaybackError(message, severity);
        }

        if (endSession) {
            internalEndSession();
        }
    }

    /**
     * Puts the session state in a notMonitored state.
     */
    public void pauseTracking() {
        if (convivaVideoAnalytics != null) {
            convivaVideoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.SEPARATE, ConvivaSdkConstants.AdType.CLIENT_SIDE);
        }
    }

    /**
     * Puts the session state from a notMonitored state into the last one tracked.
     */
    public void resumeTracking() {
        if (convivaVideoAnalytics != null) {
            convivaVideoAnalytics.reportAdBreakEnded();
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
        convivaVideoAnalytics = com.conviva.sdk.ConvivaAnalytics.buildVideoAnalytics(context);
        convivaVideoAnalytics.setCallback(videoAnalyticsCallback);
        setPlayerInfo();
    }

    private void setPlayerInfo() {
        if (convivaVideoAnalytics != null) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put(ConvivaSdkConstants.PLAYER_NAME, contentMetadataBuilder.getApplicationName());
            playerInfo.put(ConvivaSdkConstants.FRAMEWORK_NAME, "Bitmovin Player Android");
            playerInfo.put(ConvivaSdkConstants.FRAMEWORK_VERSION, playerHelper.getSdkVersionString());
            boolean isLive = contentMetadataBuilder.getStreamType() == ConvivaSdkConstants.StreamType.LIVE;
            playerInfo.put(ConvivaSdkConstants.IS_LIVE, isLive);
            convivaVideoAnalytics.setPlayerInfo(playerInfo);
        }
    }

    private ConvivaExperienceAnalytics.ICallback videoAnalyticsCallback = new ConvivaExperienceAnalytics.ICallback() {
        @Override
        public void update() {
        }

        @Override
        public void update(String s) {
        }
    };

    private void internalInitializeSession() {
        createContentMetadata();
        updateContentMetadata(metadataOverrides);
        setupPlayerStateManager();
        convivaVideoAnalytics.reportPlaybackRequested(contentMetadataBuilder.build());
        activeSession = true;
        if (metadataOverrides != null) {
            updateContentMetadata(metadataOverrides);
        }
    }

    private void updateSession() {
        if (!isSessionActive() || convivaVideoAnalytics == null) {
            return;
        }
        this.buildDynamicContentMetadata();

        VideoQuality videoQuality = bitmovinPlayer.getPlaybackVideoData();
        if (videoQuality != null) {
            int bitrate = videoQuality.getBitrate() / 1000; // in kbps
            convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.BITRATE, bitrate);
            convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RESOLUTION, videoQuality.getWidth(), videoQuality.getHeight());
            convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, Math.round(videoQuality.getFrameRate()));
        }

//        convivaVideoAnalytics.reportPlaybackMetric(contentMetadataBuilder.getStreamType().toString());
        convivaVideoAnalytics.setContentInfo(contentMetadataBuilder.build());
    }

    private void createContentMetadata() {
        SourceItem sourceItem = bitmovinPlayer.getConfig().getSourceItem();

        if (sourceItem != null) {
            contentMetadataBuilder.setAssetName(sourceItem.getTitle());
        }

        // Build custom tags
        Map<String, String> customInternTags = new HashMap<>();
        customInternTags.put("streamType", playerHelper.getStreamType());
        customInternTags.put("integrationVersion", BuildConfig.VERSION_NAME);
        contentMetadataBuilder.setCustom(customInternTags);

        this.buildDynamicContentMetadata();
    }

    private void buildDynamicContentMetadata() {
        if (bitmovinPlayer.isLive()) {
            contentMetadataBuilder.setStreamType(ConvivaSdkConstants.StreamType.LIVE);
        } else {
            contentMetadataBuilder.setStreamType(ConvivaSdkConstants.StreamType.VOD);
            contentMetadataBuilder.setDuration((int) bitmovinPlayer.getDuration());
        }
        contentMetadataBuilder.setStreamUrl(playerHelper.getStreamUrl());

        setPlayerInfo();
    }

    private void internalEndSession() {
        if (!isSessionActive() || convivaVideoAnalytics == null) {
            return;
        }

        convivaVideoAnalytics.release();
        com.conviva.sdk.ConvivaAnalytics.release();
        convivaVideoAnalytics = null;
        contentMetadataBuilder.reset();
        Log.e(TAG, "Session ended");
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
        if (!isSessionActive() || convivaVideoAnalytics == null) {
            return;
        }

        Log.d(TAG, "Transitioning to :" + state.name());
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, state);
    }

    // region Helper
    private boolean isSessionActive() {
        return activeSession;
    }

    private void trackAdEnd() {
        if (!adStarted) {
            // Do not track adEnd if no ad is was shown (possible if an error occurred)
            return;
        }
        adStarted = false;

        if (convivaVideoAnalytics != null) {
            convivaVideoAnalytics.reportAdBreakEnded();
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
            ensureConvivaSessionIsCreatedAndInitialized();

            String message = String.format("%s - %s", errorEvent.getCode(), errorEvent.getMessage());

            if (convivaVideoAnalytics != null) {
                convivaVideoAnalytics.reportPlaybackError(message, ConvivaSdkConstants.ErrorSeverity.FATAL);
            }
            internalEndSession();
        }
    };

    private OnWarningListener onWarningListener = new OnWarningListener() {
        @Override
        public void onWarning(WarningEvent warningEvent) {
            Log.d(TAG, "[Player Event] OnWarning");
            ensureConvivaSessionIsCreatedAndInitialized();

            String message = String.format("%s - %s", warningEvent.getCode(), warningEvent.getMessage());

            if (convivaVideoAnalytics != null) {
                convivaVideoAnalytics.reportPlaybackError(message, ConvivaSdkConstants.ErrorSeverity.WARNING);
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
        if (!isSessionActive() || convivaVideoAnalytics == null) {
            // Handle the case that the User seeks on the UI before play was triggered.
            // This also handles startTime feature. The same applies for onTimeShift.
            return;
        }
        Log.d(TAG, "Sending seek start event");
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_STARTED, seekTarget);
    }

    ;

    public void setSeekEnd() {
        if (!isSessionActive() || convivaVideoAnalytics == null) {
            // See comment in setSeekStart
            return;
        }
        Log.d(TAG, "Sending seek end event");
        convivaVideoAnalytics.reportPlaybackMetric(ConvivaSdkConstants.PLAYBACK.SEEK_ENDED);
    }
    // endregion

    // region Ad events
    private OnAdStartedListener onAdStartedListener = new OnAdStartedListener() {
        @Override
        public void onAdStarted(AdStartedEvent adStartedEvent) {
            adStarted = true;

            if (convivaVideoAnalytics != null) {
                convivaVideoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.SEPARATE, ConvivaSdkConstants.AdType.CLIENT_SIDE);
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