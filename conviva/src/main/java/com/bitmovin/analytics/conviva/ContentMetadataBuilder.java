package com.bitmovin.analytics.conviva;

import android.util.Log;

import com.conviva.api.ContentMetadata;

import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

class ContentMetadataBuilder {

    private static final String TAG = ContentMetadataBuilder.class.getSimpleName();

    private ContentMetadata contentMetadata;

    // internal metadata fields to enable merging / overriding
    private MetadataOverrides metadataOverrides;
    private MetadataOverrides metadata;
    private boolean playbackStarted;

    ContentMetadataBuilder() {
        contentMetadata = new ContentMetadata();
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

    public ContentMetadata build() {
        if (!playbackStarted) {
            // Asset name is only allowed to be set once
            if (contentMetadata.assetName == null) {
                contentMetadata.assetName = getAssetName();
            }

            contentMetadata.viewerId = getViewerId();
            contentMetadata.streamType = ObjectUtils.defaultIfNull(
                    metadataOverrides.getStreamType(),
                    metadata.getStreamType());

            contentMetadata.applicationName = ObjectUtils.defaultIfNull(
                    metadataOverrides.getApplicationName(),
                    metadata.getApplicationName());

            Integer duration = ObjectUtils.defaultIfNull(
                    metadataOverrides.getDuration(),
                    metadata.getDuration());
            contentMetadata.duration = duration != null ? duration : -1;

            contentMetadata.custom = getCustom();
        }

        Integer frameRate = ObjectUtils.defaultIfNull(
                metadataOverrides.getEncodedFrameRate(),
                metadata.getEncodedFrameRate());
        contentMetadata.encodedFrameRate = frameRate != null ? frameRate : -1;

        contentMetadata.defaultResource = ObjectUtils.defaultIfNull(
                metadataOverrides.getDefaultResource(),
                metadata.getDefaultResource());

        contentMetadata.streamUrl = ObjectUtils.defaultIfNull(
                metadataOverrides.getStreamUrl(),
                metadata.getStreamUrl());

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

    public void setStreamType(ContentMetadata.StreamType newValue) {
        metadata.setStreamType(newValue);
    }

    public void setApplicationName(String newValue) {
        metadata.setApplicationName(newValue);
    }

    public void setCustom(Map<String, String> newValue) {
        metadata.setCustom(newValue);
    }

    public Map<String, String> getCustom() {
        Map<String, String> customOverrides = metadataOverrides.getCustom();
        Map<String, String> customs = customOverrides != null ? customOverrides : new HashMap<String, String>();
        customs.putAll(metadata.getCustom());
        return customs;
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
        contentMetadata = new ContentMetadata();
    }
}
