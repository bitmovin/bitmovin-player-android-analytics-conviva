package com.bitmovin.analytics.conviva.ssai;

import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.media.video.quality.VideoQuality;
import com.conviva.sdk.ConvivaSdkConstants;

import java.util.HashMap;

public class DefaultPlayerAdapter implements PlayerAdapter {
    private final Player player;

    public DefaultPlayerAdapter(Player player) {
        this.player = player;
    }

    @Override
    public ConvivaSdkConstants.PlayerState getPlayerState() {
        ConvivaSdkConstants.PlayerState state;
        if (player.isPaused()) {
            state = ConvivaSdkConstants.PlayerState.PAUSED;
        } else if (player.isStalled()) {
            state = ConvivaSdkConstants.PlayerState.BUFFERING;
        } else {
            state = ConvivaSdkConstants.PlayerState.PLAYING;
        }
        return state;
    }

    @Override
    public HashMap<String, Object[]> getPlaybackVideoData() {
        HashMap<String, Object[]> videoData = new HashMap<>();
        VideoQuality playbackVideoData = player.getPlaybackVideoData();
        if (playbackVideoData != null) {
            videoData.put(ConvivaSdkConstants.PLAYBACK.RESOLUTION, new Object[]{playbackVideoData.getWidth(), playbackVideoData.getHeight()});
            videoData.put(ConvivaSdkConstants.PLAYBACK.BITRATE, new Object[]{playbackVideoData.getBitrate() / 1000});
            videoData.put(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, new Object[]{Math.round(playbackVideoData.getFrameRate())});
        }
        return videoData;
    }

    @Override
    public String getStreamTitle() {
        return player.getSource() == null ? null : player.getSource().getConfig().getTitle();
    }

    @Override
    public String getStreamType() {
        return player.getSource() == null ? null : player.getSource().getConfig().getType().name();
    }

    @Override
    public String getStreamUrl() {
        return player.getSource() == null ? null : player.getSource().getConfig().getUrl();
    }


    @Override
    public boolean isAd() {
        return player.isAd();
    }

    @Override
    public boolean isLive() {
        return player.isLive();
    }

    @Override
    public boolean isPaused() {
        return player.isPaused();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public double getDuration() {
        return player.getDuration();
    }

    @Override
    public long getPlayHeadTimeMillis() {
        if (player.isLive()) {
            double playerTimeShiftMax = player.getMaxTimeShift();
            double playerTimeShift = player.getTimeShift();
            long playerDurationMs = -(Math.round(playerTimeShiftMax * 1000));
            return playerDurationMs - -(Math.round(playerTimeShift * 1000));

        } else {
            double currentTime = player.getCurrentTime();
            return (long) (currentTime * 1000);
        }
    }

    @Override
    public void withEventEmitter(WithEventEmitter withEventEmitter) {
        withEventEmitter.call(player);
    }
}
