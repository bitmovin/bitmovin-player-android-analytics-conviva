package com.bitmovin.analytics.conviva;

import com.bitmovin.player.api.Player;

class BitmovinPlayerHelper {
    private Player player;

    BitmovinPlayerHelper(Player player) {
        this.player = player;
    }

    String getSdkVersionString() {
        return Player.getSdkVersion();
    }

    String getStreamType() {
        if (player.getSource() == null) {
            return null;
        } else {
            return player.getSource().getConfig().getType().name();
        }
    }

    String getStreamUrl() {
        if (player.getSource() == null) {
            return null;
        } else {
            return player.getSource().getConfig().getUrl();
        }
    }
}
