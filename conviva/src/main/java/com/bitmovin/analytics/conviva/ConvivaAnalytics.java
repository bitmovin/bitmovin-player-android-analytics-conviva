package com.bitmovin.analytics.conviva;

import android.content.Context;
import android.util.Log;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlayEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.ReadyEvent;
import com.bitmovin.player.api.event.data.SeekEvent;
import com.bitmovin.player.api.event.data.SeekedEvent;
import com.bitmovin.player.api.event.data.SourceLoadedEvent;
import com.bitmovin.player.api.event.data.SourceUnloadedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.VideoPlaybackQualityChangedEvent;
import com.bitmovin.player.api.event.data.WarningEvent;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlayListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnSeekListener;
import com.bitmovin.player.api.event.listener.OnSeekedListener;
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnVideoPlaybackQualityChangedListener;
import com.bitmovin.player.api.event.listener.OnWarningListener;
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

public class ConvivaAnalytics {
    private static final String TAG = "ConvivaAnalytics";
    private static final ConvivaAnalytics ourInstance = new ConvivaAnalytics();
    private Client client;
    private BitmovinPlayer bitmovinPlayer;
    private ContentMetadata contentMetadata;
    private ConvivaConfig config;
    private int sessionId = Client.NO_SESSION_KEY;
    private boolean playerStarted = false;
    private PlayerStateManager playerStateManager;
    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            Log.d(TAG, "OnSourceUnloaded");
            cleanupConvivaClient();
        }
    };
    private OnSourceLoadedListener onSourceLoadedListener = new OnSourceLoadedListener() {
        @Override
        public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
            Log.d(TAG, "OnSourceLoaded");
            createContentMetadata();
            createConvivaSession();
        }
    };
    private OnReadyListener onReadyListener = new OnReadyListener() {
        @Override
        public void onReady(ReadyEvent readyEvent) {
            Log.d(TAG, "OnReady");

            try {
                playerStarted = true;
                Log.d(TAG, "Setting Duration: " + String.valueOf(bitmovinPlayer.getDuration()));
                contentMetadata.duration = (int) bitmovinPlayer.getDuration();
                contentMetadata.encodedFrameRate = (int)bitmovinPlayer.getPlaybackVideoData().getFrameRate();
                client.updateContentMetadata(sessionId, contentMetadata);

                PlayerStateManager.PlayerState state = PlayerStateManager.PlayerState.PLAYING;
                if (bitmovinPlayer.isPaused()) {
                    state = PlayerStateManager.PlayerState.PAUSED;
                }
                transitionState(state);

            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };
    private OnVideoPlaybackQualityChangedListener onVideoPlaybackQualityChangedListener = new OnVideoPlaybackQualityChangedListener() {
        @Override
        public void onVideoPlaybackQualityChanged(VideoPlaybackQualityChangedEvent videoPlaybackQualityChangedEvent) {
            Log.d(TAG, "OnVideoPlaybackQualityChanged");
            try {
                if (videoPlaybackQualityChangedEvent != null && videoPlaybackQualityChangedEvent.getNewVideoQuality() != null) {
                    VideoQuality newVideoQuality = videoPlaybackQualityChangedEvent.getNewVideoQuality();
                    playerStateManager.setBitrateKbps(newVideoQuality.getBitrate() / 1000);
                    playerStateManager.setVideoHeight(newVideoQuality.getHeight());
                    playerStateManager.setVideoWidth(newVideoQuality.getWidth());
                }
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };
    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            Log.d(TAG, "OnPaused");
            transitionState(PlayerStateManager.PlayerState.PAUSED);
        }
    };
    private OnPlayListener onPlayListener = new OnPlayListener() {
        @Override
        public void onPlay(PlayEvent playEvent) {
            Log.d(TAG, "OnPlay");
            transitionState(PlayerStateManager.PlayerState.PLAYING);
        }
    };
    private OnSeekListener onSeekListener = new OnSeekListener() {
        @Override
        public void onSeek(SeekEvent seekEvent) {
            Log.d(TAG, "OnSeek");
            try {
                playerStateManager.setPlayerSeekStart((int) seekEvent.getSeekTarget());
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };
    private OnSeekedListener onSeekedListener = new OnSeekedListener() {
        @Override
        public void onSeeked(SeekedEvent seekedEvent) {
            Log.d(TAG, "OnSeeked");
            try {
                playerStateManager.setPlayerSeekEnd();
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };
    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            Log.d(TAG, "OnStallStarted");
            transitionState(PlayerStateManager.PlayerState.BUFFERING);
        }
    };
    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            Log.d(TAG, "OnStallEnded");
            PlayerStateManager.PlayerState state = PlayerStateManager.PlayerState.PLAYING;
            if (bitmovinPlayer.isPaused()) {
                state = PlayerStateManager.PlayerState.PAUSED;
            }
            transitionState(state);
        }
    };
    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            Log.d(TAG, "OnPlaybackFinished");
            transitionState(PlayerStateManager.PlayerState.STOPPED);
        }
    };
    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(ErrorEvent errorEvent) {
            Log.d(TAG, "OnError");
            try {
                client.reportError(errorEvent.getCode(), errorEvent.getMessage(), Client.ErrorSeverity.FATAL);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };
    private OnWarningListener onWarningListener = new OnWarningListener() {
        @Override
        public void onWarning(WarningEvent warningEvent) {
            Log.d(TAG, "OnWarning");
            try {
                client.reportError(warningEvent.getCode(), warningEvent.getMessage(), Client.ErrorSeverity.WARNING);
            } catch (ConvivaException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
    };

    private ConvivaAnalytics() {

    }

    public static ConvivaAnalytics getInstance() {
        return ourInstance;
    }

    public void attachPlayer(ConvivaConfig config, BitmovinPlayer player, Context context) {
        detachPlayer();
        this.config = config;
        bitmovinPlayer = player;
        attachBitmovinEventListeners();
        createConvivaClient(context);
    }

    private void createConvivaClient(Context context) {
        SystemInterface androidSystemInterface = AndroidSystemInterfaceFactory.buildSecure(context);
        if (androidSystemInterface.isInitialized()) {
            SystemSettings systemSettings = new SystemSettings();
            systemSettings.logLevel = SystemSettings.LogLevel.DEBUG;
            systemSettings.allowUncaughtExceptions = false;
            SystemFactory androidSystemFactory = new SystemFactory(androidSystemInterface, systemSettings);
            ClientSettings clientSettings = new ClientSettings(config.getCustomerKey());
            clientSettings.gatewayUrl = config.getGatewayUrl();
            client = new Client(clientSettings, androidSystemFactory);
        }
    }

    private void createConvivaSession(){
        try {
            sessionId = client.createSession(contentMetadata);
            playerStateManager = client.getPlayerStateManager();
            Log.i(TAG, "Created SessionID - " + sessionId);
            client.attachPlayer(sessionId, playerStateManager);
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void createContentMetadata(){
        contentMetadata = new ContentMetadata();
        contentMetadata.custom = config.getCustomData();
        contentMetadata.assetName = config.getAssetName();
        contentMetadata.viewerId = config.getViewerId();
        contentMetadata.applicationName = config.getApplicationName();

        //streamType
        if (bitmovinPlayer.isLive()) {
            contentMetadata.streamType = ContentMetadata.StreamType.LIVE;
        } else {
            contentMetadata.streamType = ContentMetadata.StreamType.VOD;
        }

        //streamUrl
        if (bitmovinPlayer.getConfig() != null && bitmovinPlayer.getConfig().getSourceItem() != null && bitmovinPlayer.getConfig().getSourceItem().getDashSource() != null) {
            contentMetadata.streamUrl = bitmovinPlayer.getConfig().getSourceItem().getDashSource().getUrl();
        } else if (bitmovinPlayer.getConfig() != null && bitmovinPlayer.getConfig().getSourceItem() != null && bitmovinPlayer.getConfig().getSourceItem().getHlsSource() != null) {
            contentMetadata.streamUrl = bitmovinPlayer.getConfig().getSourceItem().getHlsSource().getUrl();
        }

        //TODO default Resource?
        contentMetadata.defaultResource = config.getDefaultReportingResource(); //defaultReportingResource should not be null

        //TODO add encoded frame rate once its available
        // _contentMetadata.encodedFrameRate = "updated encoded frame rate value"; // encodedFrameRate is measured in frames per second
        // encodedFrameRate should be greater than 0

    }

    private void cleanupConvivaClient() {
        try {
            if(client != null) {
                client.releasePlayerStateManager(playerStateManager);
                client.cleanupSession(sessionId);
            }
        } catch (ConvivaException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        playerStateManager = null;
        playerStarted = false;
        sessionId = Client.NO_SESSION_KEY;
    }

    public void detachPlayer() {
        cleanupConvivaClient();
        removeBitmovinEventListeners();
        playerStarted = false;
        bitmovinPlayer = null;
    }

    private void attachBitmovinEventListeners() {
        bitmovinPlayer.addEventListener(onSourceLoadedListener);
        bitmovinPlayer.addEventListener(onSourceUnloadedListener);
        bitmovinPlayer.addEventListener(onErrorListener);
        bitmovinPlayer.addEventListener(onWarningListener);
        bitmovinPlayer.addEventListener(onPausedListener);
        bitmovinPlayer.addEventListener(onPlayListener);
        bitmovinPlayer.addEventListener(onSeekedListener);
        bitmovinPlayer.addEventListener(onSeekListener);
        bitmovinPlayer.addEventListener(onStallEndedListener);
        bitmovinPlayer.addEventListener(onStallStartedListener);
        bitmovinPlayer.addEventListener(onReadyListener);
        bitmovinPlayer.addEventListener(onPlaybackFinishedListener);
        bitmovinPlayer.addEventListener(onVideoPlaybackQualityChangedListener);
    }

    private void removeBitmovinEventListeners() {
        if (bitmovinPlayer != null) {
            bitmovinPlayer.removeEventListener(onSourceUnloadedListener);
            bitmovinPlayer.removeEventListener(onSourceLoadedListener);
            bitmovinPlayer.removeEventListener(onErrorListener);
            bitmovinPlayer.removeEventListener(onWarningListener);
            bitmovinPlayer.removeEventListener(onPausedListener);
            bitmovinPlayer.removeEventListener(onPlayListener);
            bitmovinPlayer.removeEventListener(onSeekedListener);
            bitmovinPlayer.removeEventListener(onSeekListener);
            bitmovinPlayer.removeEventListener(onStallEndedListener);
            bitmovinPlayer.removeEventListener(onStallStartedListener);
            bitmovinPlayer.removeEventListener(onReadyListener);
            bitmovinPlayer.removeEventListener(onPlaybackFinishedListener);
            bitmovinPlayer.removeEventListener(onVideoPlaybackQualityChangedListener);
        }
    }


    private synchronized void transitionState(PlayerStateManager.PlayerState state) {
        try {
            if (this.playerStarted) {
                if (playerStateManager.getPlayerState() != state) {
                    Log.d(TAG, "Transitioning to :" + state.name());
                    playerStateManager.setPlayerState(state);
                }
            }
        } catch (ConvivaException e) {
            Log.e(TAG, "Unable to transition state: " + e.getLocalizedMessage());
        }
    }
}
