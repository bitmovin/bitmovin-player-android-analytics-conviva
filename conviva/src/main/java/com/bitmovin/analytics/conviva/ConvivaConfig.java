package com.bitmovin.analytics.conviva;

public class ConvivaConfig {
    private String gatewayUrl;
    private String customerKey;
    private boolean debugLoggingEnabled;
    private String applicationName;
    private String viewerId;

    public ConvivaConfig(String customerKey) {
        this.customerKey = customerKey;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getCustomerKey() {
        return customerKey;
    }

    public void setCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }

    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }

    public void setDebugLoggingEnabled(boolean debugLoggingEnabled) {
        this.debugLoggingEnabled = debugLoggingEnabled;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }
}
