package com.bitmovin.analytics.conviva;

import java.util.Map;

public class ConvivaConfig {
    private String gatewayUrl;
    private boolean debugLoggingEnabled;
    private String applicationName;
    private String viewerId;
    private String defaultReportingResource;
    private Map<String,String> customData;

    public ConvivaConfig() {

    }

    public ConvivaConfig(String applicationName, String viewerId) {
        this.applicationName = applicationName;
        this.viewerId = viewerId;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, String> customData) {
        this.customData = customData;
    }

    public String getDefaultReportingResource() {
        return defaultReportingResource;
    }

    public void setDefaultReportingResource(String defaultReportingResource) {
        this.defaultReportingResource = defaultReportingResource;
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
