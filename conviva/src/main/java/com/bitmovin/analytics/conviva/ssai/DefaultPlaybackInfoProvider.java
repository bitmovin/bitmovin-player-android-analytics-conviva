package com.bitmovin.analytics.conviva.ssai;

import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.media.video.quality.VideoQuality;
import com.conviva.sdk.ConvivaSdkConstants;

import java.util.HashMap;

public class DefaultPlaybackInfoProvider implements PlaybackInfoProvider {
    private final Player player;

    public DefaultPlaybackInfoProvider(Player player) {
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
            int averageBitrate = playbackVideoData.getAverageBitrate();
            if (averageBitrate != VideoQuality.BITRATE_NO_VALUE) {
                videoData.put(ConvivaSdkConstants.PLAYBACK.AVG_BITRATE, new Object[]{averageBitrate / 1000});
            }
            videoData.put(ConvivaSdkConstants.PLAYBACK.RENDERED_FRAMERATE, new Object[]{Math.round(playbackVideoData.getFrameRate())});
        }
        return videoData;
    }
}
