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

    private Client client;
    private BitmovinPlayer bitmovinPlayer;
    private ContentMetadata contentMetadata = new ContentMetadata();
    private ConvivaConfiguration config;
    private int sessionId = Client.NO_SESSION_KEY;
    private PlayerStateManager playerStateManager;

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
        this.config = config;

        SystemInterface androidSystemInterface = AndroidSystemInterfaceFactory.buildSecure(context);
        if (androidSystemInterface.isInitialized()) {
            SystemSettings systemSettings = new SystemSettings();
            systemSettings.logLevel = SystemSettings.LogLevel.DEBUG;
            systemSettings.allowUncaughtExceptions = false;

            SystemFactory androidSystemFactory = new SystemFactory(androidSystemInterface, systemSettings);
            ClientSettings clientSettings = new ClientSettings(customerKey);

            if (config.getGatewayUrl() != null) {
                clientSettings.gatewayUrl = config.getGatewayUrl();
            }

            this.client = new Client(clientSettings, androidSystemFactory);

            attachBitmovinEventListeners();
        }
    }

    private void ensureConvivaSessionIsCreatedAndInitialized() {
        if (!isValidSession()) {
            createConvivaSession();
        }
    }

    // region Session handling
    private void setupPlayerStateManager() {
        try {
            playerStateManager = client.getPlayerStateManager();
            playerStateManager.setPlayerState(PlayerStateManager.PlayerState.STOPPED);
            playerStateManager.setPlayerType("Bitmovin Player Android");
            playerStateManager.setPlayerVersion(playerHelper.getSdkVersionString());
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void createConvivaSession() {
        try {
            createContentMetadata();
            sessionId = client.createSession(contentMetadata);
            setupPlayerStateManager();
            Log.d(TAG, "[Player Event] Created SessionID - " + sessionId);
            client.attachPlayer(sessionId, playerStateManager);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void updateSession() {
        if (!isValidSession()) {
            return;
        }

        if (!bitmovinPlayer.isLive()) {
            contentMetadata.duration = (int) bitmovinPlayer.getDuration();
        }

        if (bitmovinPlayer.isLive()) {
            contentMetadata.streamType = ContentMetadata.StreamType.LIVE;
        } else {
            contentMetadata.streamType = ContentMetadata.StreamType.VOD;
        }

        contentMetadata.streamUrl = playerHelper.getStreamUrl();

        VideoQuality videoQuality = bitmovinPlayer.getPlaybackVideoData();
        if (videoQuality != null) {
            int bitrate = videoQuality.getBitrate() / 1000; // in kbps
            try {
                playerStateManager.setBitrateKbps(bitrate);
                playerStateManager.setVideoHeight(videoQuality.getHeight());
                playerStateManager.setVideoWidth(videoQuality.getWidth());
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        try {
            client.updateContentMetadata(sessionId, contentMetadata);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void createContentMetadata() {
        SourceItem sourceItem = bitmovinPlayer.getConfig().getSourceItem();

        contentMetadata.applicationName = config.getApplicationName();
        contentMetadata.assetName = sourceItem.getTitle();
        contentMetadata.viewerId = config.getViewerId();

        // Build custom tags
        Map<String, String> customInternTags = new HashMap<>();
        customInternTags.put("streamType", playerHelper.getStreamType());
        customInternTags.putAll(config.getCustomData());
        contentMetadata.custom = customInternTags;
    }

    private void endConvivaSession() {
        if (!isValidSession()) {
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
            Log.e(TAG, "Session ended");
        }
    }
    // endregion

    // region custom Events
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
        if (!isValidSession()) {
            Log.e(TAG, "Cannot send playback event, no active monitoring session");
            return;
        }
        try {
            client.sendCustomEvent(sessionId, name, attributes);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void customEvent(BitmovinPlayerEvent event) {
        customEvent(event, new HashMap<String, Object>());
    }

    private void customEvent(BitmovinPlayerEvent event, Map<String, Object> attributes) {
        if (!isValidSession()) {
            return;
        }

        String eventName = event.getClass().getSimpleName();
        sendCustomPlaybackEvent("on" + eventName, attributes);
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

        // Ad events
        bitmovinPlayer.addEventListener(onAdStartedListener);
        bitmovinPlayer.addEventListener(onAdFinishedListener);
        bitmovinPlayer.addEventListener(onAdSkippedListener);
        bitmovinPlayer.addEventListener(onAdErrorListener);

        bitmovinPlayer.addEventListener(onVideoPlaybackQualityChangedListener);
    }

    private synchronized void transitionState(PlayerStateManager.PlayerState state) {
        if (!isValidSession()) {
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
    private boolean isValidSession() {
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
                    endConvivaSession();
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
                endConvivaSession();
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
            endConvivaSession();
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

    // region Seek events
    private OnSeekListener onSeekListener = new OnSeekListener() {
        @Override
        public void onSeek(SeekEvent seekEvent) {
            if (!isValidSession()) {
                // Handle the case that the User seeks on the UI before play was triggered.
                // This also handles startTime feature. The same applies for onTimeShift.
                return;
            }
            Log.d(TAG, "[Player Event] OnSeek");
            try {
                playerStateManager.setPlayerSeekStart((int) seekEvent.getSeekTarget() * 1000);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private OnSeekedListener onSeekedListener = new OnSeekedListener() {
        @Override
        public void onSeeked(SeekedEvent seekedEvent) {
            if (!isValidSession()) {
                // See comment in onSeek
                return;
            }
            Log.d(TAG, "[Player Event] OnSeeked");
            try {
                playerStateManager.setPlayerSeekEnd();
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };
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
