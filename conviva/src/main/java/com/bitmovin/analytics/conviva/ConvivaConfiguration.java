package com.bitmovin.analytics.conviva;

public class ConvivaConfiguration {
    private String gatewayUrl;
    private boolean debugLoggingEnabled;

    public ConvivaConfiguration() {

    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }

    public void setDebugLoggingEnabled(boolean debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled;
    }
}
