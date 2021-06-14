package com.bitmovin.analytics.conviva;

import android.util.Log;

import com.conviva.sdk.ConvivaSdkConstants;

import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

class ContentMetadataBuilder {

    private static final String TAG = ContentMetadataBuilder.class.getSimpleName();

    private Map<String, Object> contentMetadata;

    // internal metadata fields to enable merging / overriding
    private MetadataOverrides metadataOverrides;
    private MetadataOverrides metadata;
    private boolean playbackStarted;

    ContentMetadataBuilder() {
        contentMetadata = new HashMap<>();
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
            // Asset name is only allowed to be set once
            if (!contentMetadata.containsKey(ConvivaSdkConstants.ASSET_NAME)) {
                contentMetadata.put(ConvivaSdkConstants.ASSET_NAME, getAssetName());
            }

            contentMetadata.put(ConvivaSdkConstants.VIEWER_ID, getViewerId());

            Integer duration = ObjectUtils.defaultIfNull(
                    metadataOverrides.getDuration(),
                    metadata.getDuration());
            contentMetadata.put(ConvivaSdkConstants.DURATION, duration != null ? duration : -1);

            setCustom();
        }

        Integer frameRate = ObjectUtils.defaultIfNull(
                metadataOverrides.getEncodedFrameRate(),
                metadata.getEncodedFrameRate());
        contentMetadata.put(ConvivaSdkConstants.ENCODED_FRAMERATE, frameRate != null ? frameRate : -1);

        contentMetadata.put(ConvivaSdkConstants.DEFAULT_RESOURCE, ObjectUtils.defaultIfNull(
                metadataOverrides.getDefaultResource(),
                metadata.getDefaultResource()));

        contentMetadata.put(ConvivaSdkConstants.STREAM_URL, ObjectUtils.defaultIfNull(
                metadataOverrides.getStreamUrl(),
                metadata.getStreamUrl()));

        return contentMetadata;
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

    public ConvivaSdkConstants.StreamType getStreamType() {
        return ObjectUtils.defaultIfNull(metadataOverrides.getStreamType(), metadata.getStreamType());
    }

    public void setStreamType(ConvivaSdkConstants.StreamType newValue) {
        metadata.setStreamType(newValue);
    }

    public String getApplicationName() {
        return ObjectUtils.defaultIfNull(metadataOverrides.getApplicationName(), metadata.getApplicationName());
    }

    public void setApplicationName(String newValue) {
        metadata.setApplicationName(newValue);
    }

    public void setCustom(Map<String, String> newValue) {
        metadata.setCustom(newValue);
    }

    public void setCustom() {
        // merge internal and override metadata key-value pairs
        // with override values having higher precedence
        Map<String, String> customInternals = metadata.getCustom();
        if (customInternals != null) {
            contentMetadata.putAll(customInternals);
        }
        Map<String, String> customOverrides = metadataOverrides.getCustom();
        if (customOverrides != null) {
            contentMetadata.putAll(customOverrides);
        }
    }

    public void setDuration(Integer newValue) {
        metadata.setDuration(newValue);
    }

    public void setEncodedFrameRate(Integer newValue) {
        metadata.setEncodedFrameRate(newValue);
    }

    public void setdefaultResource(String newValue) {
        metadata.setDefaultResource(newValue);
    }

    public void setStreamUrl(String newValue) {
        metadata.setStreamUrl(newValue);
    }

    public void reset() {
        metadataOverrides = new MetadataOverrides();
        metadata = new MetadataOverrides();
        playbackStarted = false;
        contentMetadata.clear();
    }
}
