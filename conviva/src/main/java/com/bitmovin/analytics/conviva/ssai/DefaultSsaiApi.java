package com.bitmovin.analytics.conviva.ssai;

import android.util.Log;

import com.conviva.sdk.ConvivaAdAnalytics;
import com.conviva.sdk.ConvivaSdkConstants;
import com.conviva.sdk.ConvivaVideoAnalytics;

import java.util.HashMap;
import java.util.Map;


public class DefaultSsaiApi implements SsaiApi {
    private static final String TAG = "DefaultSsaiApi";
    private final ConvivaVideoAnalytics convivaVideoAnalytics;
    private final ConvivaAdAnalytics convivaAdAnalytics;
    private final PlaybackStateProvider player;

    public DefaultSsaiApi(ConvivaVideoAnalytics convivaVideoAnalytics, ConvivaAdAnalytics convivaAdAnalytics, PlaybackStateProvider player) {
        this.convivaVideoAnalytics = convivaVideoAnalytics;
        this.convivaAdAnalytics = convivaAdAnalytics;
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
        isAdBreakActive = false;
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
        Log.d(TAG, "Server side ad started");
        convivaAdAnalytics.reportAdStarted(convertToConvivaAdInfo(adInfo));
        convivaAdAnalytics.reportAdMetric(ConvivaSdkConstants.PLAYBACK.PLAYER_STATE, player.getPlayerState());
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

        convivaAdAnalytics.setAdInfo(convertToConvivaAdInfo(adInfo));
    }

    private static Map<String, Object> convertToConvivaAdInfo(AdInfo adInfo) {
        HashMap<String, Object> convivaAdInfo = new HashMap<>();
        convivaAdInfo.put("c3.ad.id", "NA");
        convivaAdInfo.put("c3.ad.system", "NA");
        convivaAdInfo.put("c3.ad.mediaFileApiFramework", "NA");
        convivaAdInfo.put("c3.ad.firstAdSystem", "NA");
        convivaAdInfo.put("c3.ad.firstAdId", "NA");
        convivaAdInfo.put("c3.ad.firstCreativeId", "NA");
        convivaAdInfo.put("c3.ad.technology", "Server Side");

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
        if (adInfo.getAdditionalMetadata() != null) {
            convivaAdInfo.putAll(adInfo.getAdditionalMetadata());
        }

        return convivaAdInfo;
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
