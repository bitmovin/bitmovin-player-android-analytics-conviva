package com.bitmovin.analytics.conviva.ssai;

import java.util.Map;

/**
 * Enables reporting of server-side ad breaks and ads.
 */
public interface SsaiApi {
    /**
     * Checks if a server-side ad break is currently active.
     *
     * @return <code>true</code> if a server-side ad break is active, <code>false</code> otherwise.
     */
    boolean isAdBreakActive();

    /**
     * Reports the start of a server-side ad break. Must be called before the first ad starts.
     * Has no effect if a server-side ad break is already playing.
     */
    void reportAdBreakStarted();

    /**
     * Reports the start of a server-side ad break. Must be called before the first ad starts.
     * Has no effect if a server-side ad break is already playing.
     *
     * @param adBreakInfo Map containing metadata about the server-side ad break. Can be <code>null</code>.
     */
    void reportAdBreakStarted(Map<String, Object> adBreakInfo);

    /**
     * Reports the end of a server-side ad break. Must be called after the last ad has finished.
     * Has no effect if no server-side ad break is currently active.
     */
    void reportAdBreakFinished();

    /**
     * Reports the start of a server-side ad.
     *
     * @param adInfo Object containing metadata about the server-side ad.
     */
    void reportAdStarted(AdInfo adInfo);

    /**
     * Reports the end of a server-side ad.
     * Has no effect if no server-side ad is currently playing.
     */
    void reportAdFinished();

    /**
     * Reports that the current ad was skipped.
     * Has no effect if no server-side ad is playing.
     */
    void reportAdSkipped();

    /**
     * Updates the ad metadata during an active client-side or server-side ad.
     * Has no effect if no server-side ad is playing.
     *
     * @param adInfo Object containing metadata about the ad.
     */
    void updateAdInfo(AdInfo adInfo);

    /**
     * Represents metadata for an ad.
     */
    class AdInfo {
        private String title;
        private double duration;
        private String id;
        private String adSystem;
        private AdPosition position;
        private boolean isSlate = false;
        private String adStitcher;
        private Map<String, Object> additionalMetadata;

        public double getDuration() {
            return duration;
        }

        /**
         * Duration of the ad, in seconds.
         */
        public void setDuration(double duration) {
            this.duration = duration;
        }

        public String getId() {
            return id;
        }

        /**
         * The ad ID extracted from the ad server that contains the ad creative.
         */
        public void setId(String id) {
            this.id = id;
        }

        public String getAdSystem() {
            return adSystem;
        }

        /**
         * The name of the ad system (i.e., the ad server).
         */
        public void setAdSystem(String adSystem) {
            this.adSystem = adSystem;
        }

        public AdPosition getPosition() {
            return position;
        }

        /**
         * The position of the ad.
         */
        public void setPosition(AdPosition position) {
            this.position = position;
        }

        public boolean isSlate() {
            return isSlate;
        }

        /**
         * Indicates whether this ad is a slate or not. Set to <code>true</code> for slate and <code>false</code> for a regular ad.
         * Default is <code>false</code>.
         */
        public void setSlate(boolean slate) {
            isSlate = slate;
        }

        public String getAdStitcher() {
            return adStitcher;
        }

        /**
         * The name of the ad stitcher.
         */
        public void setAdStitcher(String adStitcher) {
            this.adStitcher = adStitcher;
        }

        public Map<String, Object> getAdditionalMetadata() {
            return additionalMetadata;
        }

        /**
         * Additional ad metadata. This is a map of key-value pairs that can be used to pass additional metadata about the ad.
         * A list of ad metadata can be found here: <a href="https://pulse.conviva.com/learning-center/content/sensor_developer_center/sensor_integration/android/android_stream_sensor.htm#IntegrateAdManagers">Conviva documentation</a>
         * <p>
         * Metadata provided here will supersede any data provided in the ad break info.
         */
        public void setAdditionalMetadata(Map<String, Object> additionalMetadata) {
            this.additionalMetadata = additionalMetadata;
        }

        public String getTitle() {
            return title;
        }

        /**
         * The title of the ad.
         */
        public void setTitle(String title) {
            this.title = title;
        }
    }
}
