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
    private Map<String, Object> additionalStandardTags;

    // Dynamic
    private Integer encodedFrameRate;
    private String defaultResource;
    private String streamUrl;
    private String imaSdkVersion;

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

    public Map<String, Object> getAdditionalStandardTags() {
        return additionalStandardTags;
    }

    /**
     * Standard Conviva tags that aren't covered by the other fields in this class.
     * List of tags can be found here: <a href="https://pulse.conviva.com/learning-center/content/sensor_developer_center/sensor_integration/android/android_stream_sensor.htm#PredefinedVideoandContentMetadata">Pre-defined Video and Content Metadata</a>
     */
    public void setAdditionalStandardTags(Map<String, Object> additionalStandardTags) {
        this.additionalStandardTags = additionalStandardTags;
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

    public String getImaSdkVersion() {
        return imaSdkVersion;
    }

    /**
     * Set the IMA SDK version to be tracked with client side ads of type
     * {@link com.bitmovin.player.api.advertising.AdSourceType#Ima}.
     */
    public void setImaSdkVersion(String imaSdkVersion) {
        this.imaSdkVersion = imaSdkVersion;
    }
}
