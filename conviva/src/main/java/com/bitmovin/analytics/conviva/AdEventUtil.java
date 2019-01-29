package com.bitmovin.analytics.conviva;

import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.conviva.api.Client;

class AdEventUtil {
    static final String positionRegexPattern = "pre|post|[0-9]+%|([0-9]+:)?([0-9]+:)?[0-9]+(\\.[0-9]+)?";
    private static final int[] TO_SECONDS_FACTOR = {1, 60, 60 * 60, 60 * 60 * 24};

    static Client.AdPosition parseAdPosition(AdStartedEvent event, double contentDuration) {
        String position = event.getPosition();

        if (position == null) {
            return Client.AdPosition.PREROLL;
        }

        if (position.contains("%")) {
            return parsePercentage(position);
        }

        if (position.contains(":")) {
            return parseTime(position, contentDuration);
        }

        return parseString(position.toLowerCase());
    }

    private static Client.AdPosition parsePercentage(String position) {
        position = position.replace("%", "");
        double percentageValue = Double.parseDouble(position);

        if (percentageValue == 0) {
            return Client.AdPosition.PREROLL;
        } else if (percentageValue == 100) {
            return Client.AdPosition.POSTROLL;
        } else {
            return Client.AdPosition.MIDROLL;
        }
    }

    private static Client.AdPosition parseTime(String position, double contentDuration) {
        String[] stringParts = position.split(":");
        double seconds = 0;

        for (int i = 0; i < stringParts.length; i++)
        {
            seconds += Double.parseDouble(stringParts[i]) * TO_SECONDS_FACTOR[i];
        }

        if (seconds == 0) {
            return Client.AdPosition.PREROLL;
        } else if (seconds == contentDuration) {
            return Client.AdPosition.POSTROLL;
        } else {
            return Client.AdPosition.MIDROLL;
        }
    }

    private static Client.AdPosition parseString(String position) {
        switch (position) {
            case "pre":
                return Client.AdPosition.PREROLL;
            case "post":
                return Client.AdPosition.POSTROLL;
            default:
                return Client.AdPosition.MIDROLL;
        }
    }
}
