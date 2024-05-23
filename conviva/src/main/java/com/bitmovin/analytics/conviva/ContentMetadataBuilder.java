package com.bitmovin.analytics.conviva;

import android.util.Log;
import com.conviva.sdk.ConvivaSdkConstants;
import java.util.HashMap;
import java.util.Map;

class ContentMetadataBuilder {

    private static final String TAG = ContentMetadataBuilder.class.getSimpleName();

    private Map<String, Object> contentInfo;

    // internal metadata fields to enable merging / overriding
    private MetadataOverrides metadataOverrides;
    private MetadataOverrides metadata;
    private boolean playbackStarted;

    ContentMetadataBuilder() {
        contentInfo = new HashMap<String, Object>();
        metadata = new MetadataOverrides();
        metadataOverrides = new MetadataOverrides();
    }

    /**
     * This method is used for custom content metadata updates during / before a session.
     *
     * @param metadataOverrides
     */
    public void setOverrides(MetadataOverrides metadataOverrides) {
        if (playbackStarted) {
            Log.i(TAG, "[ Conviva Analytics ] Playback has started. Only some metadata attributes will be updated");
        }

        this.metadataOverrides = metadataOverrides;
    }

    public void setPlaybackStarted(boolean value) {
        playbackStarted = value;
    }

    public Map<String, Object> build() {

        if (!playbackStarted) {
            if (!contentInfo.containsKey(ConvivaSdkConstants.ASSET_NAME)) {
                contentInfo.put(ConvivaSdkConstants.ASSET_NAME, getAssetName());
            }

            contentInfo.put(ConvivaSdkConstants.VIEWER_ID, getViewerId());

            ConvivaSdkConstants.StreamType streamType = ObjectUtils.defaultIfNull(
                    metadataOverrides.getStreamType(),
                    metadata.getStreamType());
            boolean isLive = streamType == ConvivaSdkConstants.StreamType.LIVE;
            contentInfo.put(ConvivaSdkConstants.IS_LIVE, isLive);

            String applicationName = ObjectUtils.defaultIfNull(
                    metadataOverrides.getApplicationName(),
                    metadata.getApplicationName());
            contentInfo.put(ConvivaSdkConstants.PLAYER_NAME, applicationName);

            Integer duration = ObjectUtils.defaultIfNull(
                    metadataOverrides.getDuration(),
                    metadata.getDuration());
            Integer convivaDuration = duration != null ? duration : -1;
            if (convivaDuration > 0) {
                contentInfo.put(ConvivaSdkConstants.DURATION, convivaDuration);
            }

            contentInfo.putAll(getCustom());
            contentInfo.putAll(getAdditionalStandardTags());
        }

        Integer frameRate = ObjectUtils.defaultIfNull(
                metadataOverrides.getEncodedFrameRate(),
                metadata.getEncodedFrameRate());
        contentInfo.put(ConvivaSdkConstants.ENCODED_FRAMERATE, frameRate != null ? frameRate : -1);

        String defaultResource = ObjectUtils.defaultIfNull(
                metadataOverrides.getDefaultResource(),
                metadata.getDefaultResource());
        contentInfo.put(ConvivaSdkConstants.DEFAULT_RESOURCE, defaultResource);

        String streamUrl = ObjectUtils.defaultIfNull(
                metadataOverrides.getStreamUrl(),
                metadata.getStreamUrl());
        contentInfo.put(ConvivaSdkConstants.STREAM_URL, streamUrl);

        return contentInfo;
    }

    public void setAssetName(String newValue) {
        metadata.setAssetName(newValue);
    }

    public String getAssetName() {
        return ObjectUtils.defaultIfNull(metadataOverrides.getAssetName(), metadata.getAssetName());
    }

    public void setViewerId(String newValue) {
        metadata.setViewerId(newValue);
    }

    public String getViewerId() {
        return ObjectUtils.defaultIfNull(metadataOverrides.getViewerId(), metadata.getViewerId());
    }

    public void setStreamType(ConvivaSdkConstants.StreamType newValue) {
        metadata.setStreamType(newValue);
    }

    public void setApplicationName(String newValue) {
        metadata.setApplicationName(newValue);
    }

    public void setCustom(Map<String, String> newValue) {
        metadata.setCustom(newValue);
    }

    public Map<String, String> getCustom() {
        // merge internal and override metadata key-value pairs
        // with override values having higher precedence
        Map<String, String> customInternals = metadata.getCustom();
        Map<String, String> customs = customInternals != null ? customInternals : new HashMap<String, String>();
        Map<String, String> customOverrides = metadataOverrides.getCustom();
        if (customOverrides != null) {
            customs.putAll(customOverrides);
        }
        return customs;
    }

    public void setAdditionalStandardTags(Map<String, Object> newValue) {
        metadata.setAdditionalStandardTags(newValue);
    }

    public Map<String, Object> getAdditionalStandardTags() {
        // merge internal and override metadata key-value pairs
        // with override values having higher precedence
        Map<String, Object> internalStandardTags = metadata.getAdditionalStandardTags();
        Map<String, Object> additionalStandardTags = internalStandardTags != null ? internalStandardTags : new HashMap<>();
        Map<String, Object> additionalStandardTagsOverrides = metadataOverrides.getAdditionalStandardTags();
        if (additionalStandardTagsOverrides != null) {
            additionalStandardTags.putAll(additionalStandardTagsOverrides);
        }
        return additionalStandardTags;
    }

    public void setDuration(Integer newValue) {
        metadata.setDuration(newValue);
    }

    public void setEncodedFrameRate(Integer newValue) {
        metadata.setEncodedFrameRate(newValue);
    }

    public void setDefaultResource(String newValue) {
        metadata.setDefaultResource(newValue);
    }

    public void setStreamUrl(String newValue) {
        metadata.setStreamUrl(newValue);
    }

    public void reset() {
        metadataOverrides = new MetadataOverrides();
        metadata = new MetadataOverrides();
        playbackStarted = false;
        contentInfo = new HashMap<String, Object>();
    }
}
