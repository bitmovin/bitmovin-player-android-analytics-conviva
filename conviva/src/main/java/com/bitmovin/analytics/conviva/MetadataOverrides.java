package com.bitmovin.analytics.conviva;

import com.conviva.sdk.ConvivaSdkConstants;

import java.util.Map;

public class MetadataOverrides {
    // Can only be set once
    private String assetName;

    // Can only be set before playback started
    private String viewerId;
    private ConvivaSdkConstants.StreamType streamType;
    private String applicationName;
    private Map<String, String> custom;
    private Integer duration;

    // Dynamic
    private Integer encodedFrameRate;
    private String defaultResource;
    private String streamUrl;

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }

    public ConvivaSdkConstants.StreamType getStreamType() {
        return streamType;
    }

    public void setStreamType(ConvivaSdkConstants.StreamType streamType) {
        this.streamType = streamType;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Map<String, String> getCustom() {
        return custom;
    }

    public void setCustom(Map<String, String> custom) {
        this.custom = custom;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getEncodedFrameRate() {
        return encodedFrameRate;
    }

    public void setEncodedFrameRate(Integer encodedFrameRate) {
        this.encodedFrameRate = encodedFrameRate;
    }

    public String getDefaultResource() {
        return defaultResource;
    }

    public void setDefaultResource(String defaultResource) {
        this.defaultResource = defaultResource;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
}
