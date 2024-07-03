package com.bitmovin.analytics.conviva.ssai;

import com.conviva.sdk.ConvivaSdkConstants;

import java.util.HashMap;

public interface PlaybackInfoProvider {
    ConvivaSdkConstants.PlayerState getPlayerState();

    HashMap<String, Object[]> getPlaybackVideoData();
}
