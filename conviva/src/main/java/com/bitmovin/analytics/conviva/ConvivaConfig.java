package com.bitmovin.analytics.conviva;

import java.util.Map;

public class ConvivaConfig {
    private String gatewayUrl;
    private String customerKey;
    private boolean debugLoggingEnabled;
    private String applicationName;
    private String viewerId;
    private String assetName;
    private String defaultReportingResource;
    private Map<String,String> customData;

    public ConvivaConfig(String customerKey, String gatewayUrl, String applicationName, String viewerId, String assetName) {
        this.customerKey = customerKey;
        this.gatewayUrl = gatewayUrl;
        this.applicationName = applicationName;
        this.viewerId = viewerId;
        this.assetName = assetName;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void setCustomData(Map<String, String> customData) {
        this.customData = customData;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
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
