package com.bitmovin.analytics.conviva.ssai;

import com.bitmovin.player.api.Player;
import com.bitmovin.player.api.media.video.quality.VideoQuality;
import com.conviva.sdk.ConvivaSdkConstants;

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
    public VideoQuality getPlaybackVideoData() {
        return player.getPlaybackVideoData();
    }
}
