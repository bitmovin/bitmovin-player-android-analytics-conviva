package com.bitmovin.analytics.conviva;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BuildConfig;
import com.bitmovin.player.config.media.SourceItem;

class BitmovinPlayerHelper {
    private BitmovinPlayer player;

    BitmovinPlayerHelper(BitmovinPlayer player) {
        this.player = player;
    }

    String getSdkVersionString() {
        return BuildConfig.VERSION_NAME;
    }

    String getStreamType() {
        SourceItem sourceItem = player.getConfig().getSourceItem();
        return sourceItem.getType().name();
    }

    String getStreamUrl() {
        SourceItem sourceItem = player.getConfig().getSourceItem();

        switch (sourceItem.getType()) {
            case DASH:
                return sourceItem.getDashSource().getUrl();
            case HLS:
                return sourceItem.getHlsSource().getUrl();
            case PROGRESSIVE:
                return sourceItem.getProgressiveSources().get(0).getUrl();
            case SMOOTH:
                return sourceItem.getSmoothSource().getUrl();
            default:
                return "Unknown streamUrl";
        }
    }
}
