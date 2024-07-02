package com.bitmovin.analytics.conviva.ssai;

import com.conviva.sdk.ConvivaSdkConstants;

public interface PlaybackStateProvider {
    ConvivaSdkConstants.PlayerState getPlayerState();
}
