package com.bitmovin.analytics.conviva;

import com.bitmovin.player.api.Player;
import com.bitmovin.player.BuildConfig;
import com.bitmovin.player.api.source.SourceConfig;

class BitmovinPlayerHelper {
    private Player player;

    BitmovinPlayerHelper(Player player) {
        this.player = player;
    }

    String getSdkVersionString() {
        return BuildConfig.VERSION_NAME;
    }

    String getStreamType() {
        if (player.getSource() == null || player.getSource().getConfig() == null) {
            return null;
        } else {
            return player.getSource().getConfig().getType().name();
        }
    }

    String getStreamUrl() {
        if (player.getSource() == null || player.getSource().getConfig() == null) {
            return null;
        } else {
            return player.getSource().getConfig().getUrl();
        }
    }
}
