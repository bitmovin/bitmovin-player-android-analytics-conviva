package com.bitmovin.analytics.conviva;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BuildConfig;
import com.bitmovin.player.config.media.SourceItem;

enum StreamType {
    DASH("DASH"),
    HLS("HLS"),
    progressive("progressive"),
    none("none");

    private final String text;

    StreamType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}

class BitmovinPlayerHelper {
    // For now it's more a SourceItemHelper but lets keep the name
    private SourceItem sourceItem;

    BitmovinPlayerHelper(BitmovinPlayer player) {
        this.sourceItem = player.getConfig().getSourceItem();
    }

    String getSdkVersionString() {
        return BuildConfig.VERSION_NAME;
    }

    StreamType getStreamType() {
        if (sourceItem.getDashSource() != null) {
            return StreamType.DASH;
        } else if (sourceItem.getHlsSource() != null) {
            return StreamType.HLS;
        } else if (sourceItem.getProgressiveSources() != null &&
                   !sourceItem.getProgressiveSources().isEmpty()) {
            return StreamType.progressive;
        }
        return StreamType.none;
    }

    String getStreamUrl() {
        switch (getStreamType()) {
            case DASH:
                return sourceItem.getDashSource().getUrl();
            case HLS:
                return sourceItem.getHlsSource().getUrl();
            case progressive:
                return sourceItem.getProgressiveSources().get(0).getUrl();
            default:
                return "Unknown streamUrl";
        }
    }
}
