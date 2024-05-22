package com.bitmovin.analytics.conviva;

import androidx.annotation.Nullable;
import com.bitmovin.player.api.Player;

class BitmovinPlayerHelper {
    private Player player;

    BitmovinPlayerHelper(Player player) {
        this.player = player;
    }

    String getSdkVersionString() {
        return Player.getSdkVersion();
    }

    @Nullable
    String getStreamType() {
        if (player.getSource() == null) {
            return null;
        } else {
            return player.getSource().getConfig().getType().name();
        }
    }

    @Nullable
    String getStreamUrl() {
        if (player.getSource() == null) {
            return null;
        } else {
            return player.getSource().getConfig().getUrl();
        }
    }
}
