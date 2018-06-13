package com.bitmovin.analytics.conviva;

        import com.conviva.api.Client;

public class ConvivaAnalytics {
    private static final ConvivaAnalytics ourInstance = new ConvivaAnalytics();

    public static ConvivaAnalytics getInstance() {
        return ourInstance;
    }

    private Client client;

    private ConvivaAnalytics() {

    }
}
