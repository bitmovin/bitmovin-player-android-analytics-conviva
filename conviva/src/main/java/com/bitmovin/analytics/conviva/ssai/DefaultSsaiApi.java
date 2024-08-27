package com.bitmovin.analytics.conviva.ssai;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration;
import com.bitmovin.analytics.conviva.PlayerDecorator;
import com.conviva.sdk.ConvivaAdAnalytics;
import com.conviva.sdk.ConvivaSdkConstants;
import com.conviva.sdk.ConvivaVideoAnalytics;

import java.util.HashMap;
import java.util.Map;


public class DefaultSsaiApi implements SsaiApi {
    private static final String TAG = "DefaultSsaiApi";
    private final ConvivaVideoAnalytics convivaVideoAnalytics;
    private final ConvivaAdAnalytics convivaAdAnalytics;
    @Nullable
    private PlayerDecorator player;

    public DefaultSsaiApi(
        ConvivaVideoAnalytics convivaVideoAnalytics,
        ConvivaAdAnalytics convivaAdAnalytics
    ) {
        this.convivaVideoAnalytics = convivaVideoAnalytics;
        this.convivaAdAnalytics = convivaAdAnalytics;
    }

    public void setPlayer(@NonNull PlayerDecorator player) {
        this.player = player;
    }

    private boolean isAdBreakActive = false;

    @Override
    public boolean isAdBreakActive() {
        return isAdBreakActive;
    }

    @Override
    public void reportAdBreakStarted() {
        reportAdBreakStarted(null);
    }

    public void reset() {
        reportAdFinished();
        reportAdBreakFinished();
    }

    @Override
    public void reportAdBreakStarted(Map<String, Object> adBreakInfo) {
        if (isAdBreakActive) {
            Log.d(TAG, "Server side ad break already active");
            return;
        }
        isAdBreakActive = true;
        Log.d(TAG, "Server side ad break started");
        convivaVideoAnalytics.reportAdBreakStarted(ConvivaSdkConstants.AdPlayer.CONTENT, ConvivaSdkConstants.AdType.SERVER_SIDE, adBreakInfo);
    }


    @Override
    public void reportAdBreakFinished() {
        if (!isAdBreakActive) {
            Log.d(TAG, "No server side ad break active");
            return;
        }
        isAdBreakActive = false;
        Log.d(TAG, "Server side ad break finished");
        convivaVideoAnalytics.reportAdBreakEnded();
    }


    @Override
    public void reportAdStarted(AdInfo adInfo) {
        if (!isAdBreakActive) {
            Log.d(TAG, "No server side ad break active");
            return;
        }
        if (player == null) {
            Log.w(TAG, "Player not yet set. Cannot report ad started.");
            return;
        }
        Log.d(TAG, "Server side ad started");
        convivaAdAnalytics.reportAdStarted(convertToConvivaAdInfo(adInfo, convivaVideoAnalytics.getMetadataInfo()));
        reportInitialAdMetrics(player);
    }

    private void reportInitialAdMetrics(@NonNull PlayerDecorator player) {
        convivaAdAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, player.getPlayerState());
        HashMap<String, Object[]> playbackVideoData = player.getPlaybackVideoData();
        for (Map.Entry<String, Object[]> entry : playbackVideoData.entrySet()) {
            convivaAdAnalytics.reportAdMetric(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void reportAdFinished() {
        if (!isAdBreakActive) {
            Log.d(TAG, "No ad break active");
            return;
        }
        Log.d(TAG, "Server side ad finished");
        convivaAdAnalytics.reportAdEnded();
    }

    @Override
    public void reportAdSkipped() {
        if (!isAdBreakActive) {
            Log.d(TAG, "No ad break active");
            return;
        }
        Log.d(TAG, "Server side ad skipped");
        convivaAdAnalytics.reportAdSkipped();
    }

    @Override
    public void updateAdInfo(AdInfo adInfo) {
        if (!isAdBreakActive) {
            Log.d(TAG, "No ad break active");
            return;
        }
        Log.d(TAG, "Setting ad info");

        convivaAdAnalytics.setAdInfo(convertToConvivaAdInfo(adInfo, convivaVideoAnalytics.getMetadataInfo()));
    }

    private static Map<String, Object> convertToConvivaAdInfo(
            AdInfo adInfo,
            Map<String, Object> mainContentMetadata
    ) {
        HashMap<String, Object> convivaAdInfo = new HashMap<>();

        setDefaults(convivaAdInfo);
        setFromMainContent(mainContentMetadata, convivaAdInfo);
        setFromAdInfo(adInfo, convivaAdInfo);

        if (adInfo.getAdditionalMetadata() != null) {
            convivaAdInfo.putAll(adInfo.getAdditionalMetadata());
        }

        return convivaAdInfo;
    }

    private static void setDefaults(HashMap<String, Object> convivaAdInfo) {
        convivaAdInfo.put("c3.ad.id", "NA");
        convivaAdInfo.put("c3.ad.system", "NA");
        convivaAdInfo.put("c3.ad.mediaFileApiFramework", "NA");
        convivaAdInfo.put("c3.ad.firstAdSystem", "NA");
        convivaAdInfo.put("c3.ad.firstAdId", "NA");
        convivaAdInfo.put("c3.ad.firstCreativeId", "NA");
        convivaAdInfo.put("c3.ad.technology", "Server Side");
    }

    private static void setFromMainContent(Map<String, Object> mainContentMetadata, HashMap<String, Object> convivaAdInfo) {
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaSdkConstants.STREAM_URL);
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaSdkConstants.ASSET_NAME);
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaSdkConstants.IS_LIVE);
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaSdkConstants.DEFAULT_RESOURCE);
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaSdkConstants.ENCODED_FRAMERATE);
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaAnalyticsIntegration.STREAM_TYPE);
        maybeCopyFromMainContent(mainContentMetadata, convivaAdInfo, ConvivaAnalyticsIntegration.INTEGRATION_VERSION);
    }

    private static void maybeCopyFromMainContent(Map<String, Object> mainContentMetadata, HashMap<String, Object> convivaAdInfo, String key) {
        if (mainContentMetadata.containsKey(key)) {
            convivaAdInfo.put(key, mainContentMetadata.get(key));
        }
    }

    private static void setFromAdInfo(AdInfo adInfo, HashMap<String, Object> convivaAdInfo) {
        convivaAdInfo.put("c3.ad.isSlate", adInfo.isSlate());

        if (adInfo.getTitle() != null) {
            convivaAdInfo.put(ConvivaSdkConstants.ASSET_NAME, adInfo.getTitle());
        }
        if (adInfo.getDuration() != 0) {
            convivaAdInfo.put(ConvivaSdkConstants.DURATION, adInfo.getDuration());
        }
        if (adInfo.getId() != null) {
            convivaAdInfo.put("c3.ad.id", adInfo.getId());
        }
        if (adInfo.getAdSystem() != null) {
            convivaAdInfo.put("c3.ad.system", adInfo.getAdSystem());
        }
        if (adInfo.getPosition() != null) {
            convivaAdInfo.put("c3.ad.position", toConvivaAdPosition(adInfo.getPosition()));
        }
        if (adInfo.getAdStitcher() != null) {
            convivaAdInfo.put("c3.ad.stitcher", adInfo.getAdStitcher());
        }
    }

    private static ConvivaSdkConstants.AdPosition toConvivaAdPosition(AdPosition position) {
        switch (position) {
            case PREROLL:
                return ConvivaSdkConstants.AdPosition.PREROLL;
            case POSTROLL:
                return ConvivaSdkConstants.AdPosition.POSTROLL;
            default:
                return ConvivaSdkConstants.AdPosition.MIDROLL;
        }
    }
}
