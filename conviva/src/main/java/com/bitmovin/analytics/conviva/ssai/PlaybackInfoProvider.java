package com.bitmovin.analytics.conviva.ssai;

import com.bitmovin.player.api.media.video.quality.VideoQuality;
import com.conviva.sdk.ConvivaSdkConstants;

public interface PlaybackInfoProvider {
    ConvivaSdkConstants.PlayerState getPlayerState();
    VideoQuality getPlaybackVideoData();
}
