package com.bitmovin.analytics.conviva.ssai;

import com.conviva.sdk.ConvivaSdkConstants;

import java.util.HashMap;

public interface PlayerAdapter {
    ConvivaSdkConstants.PlayerState getPlayerState();

    HashMap<String, Object[]> getPlaybackVideoData();

    boolean isAd();

    String getStreamTitle();

    String getStreamType();

    String getStreamUrl();

    long getPlayHeadTimeMillis();


    boolean isLive();

    double getDuration();


    void withEventEmitter(WithEventEmitter withEventEmitter);

    boolean isPaused();

    boolean isPlaying();

}
