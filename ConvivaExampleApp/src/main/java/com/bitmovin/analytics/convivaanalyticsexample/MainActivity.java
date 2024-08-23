package com.bitmovin.analytics.convivaanalyticsexample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.bitmovin.analytics.conviva.ConvivaAnalyticsIntegration;
import com.bitmovin.analytics.conviva.ConvivaConfig;
import com.bitmovin.analytics.conviva.MetadataOverrides;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.PlayerView;
import com.bitmovin.player.api.PlayerBuilder;
import com.bitmovin.player.api.source.SourceConfig;
import com.bitmovin.player.api.PlayerConfig;
import com.bitmovin.player.api.advertising.AdItem;
import com.bitmovin.player.api.advertising.AdSource;
import com.bitmovin.player.api.advertising.AdSourceType;
import com.bitmovin.player.api.advertising.AdvertisingConfig;
import com.bitmovin.player.api.source.SourceType;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // UI
    private Button pauseTrackingButton;
    private Button resumeTrackingButton;
    private Button releaseButton;
    private Button createButton;
    private Button sendCustomEventButton;
    private Switch includeAdsSwitch;

    // Conviva
    private static final String customerKey = "YOUR-CUSTOMER-KEY";
    private static String gatewayUrl; // Set only in debug mode
    private ConvivaAnalyticsIntegration convivaAnalyticsIntegration;

    // Player
    private Player bitmovinPlayer;
    private PlayerView bitmovinPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pauseTrackingButton = findViewById(R.id.pause_tracking_button);
        pauseTrackingButton.setOnClickListener(this);
        resumeTrackingButton = findViewById(R.id.resume_tracking_button);
        resumeTrackingButton.setOnClickListener(this);
        releaseButton = findViewById(R.id.release_button);
        releaseButton.setOnClickListener(this);
        createButton = findViewById(R.id.create_button);
        createButton.setOnClickListener(this);
        sendCustomEventButton = findViewById(R.id.custom_event_button);
        sendCustomEventButton.setOnClickListener(this);
        includeAdsSwitch = findViewById(R.id.include_ads_switch);

        this.setupBitmovinPlayer();
    }

    protected void setupBitmovinPlayer() {
        this.bitmovinPlayer = new PlayerBuilder(this)
                .setPlayerConfig(buildPlayerConfiguration())
                .disableAnalytics()
                .build();
        this.bitmovinPlayerView = new PlayerView(this, this.bitmovinPlayer);

        LinearLayout playerUIView = this.findViewById(R.id.bitmovinPlayerUIView);
        playerUIView.addView(bitmovinPlayerView);

        // Create your ConvivaConfig object
        ConvivaConfig convivaConfig = new ConvivaConfig();

        // Set only in debug mode
        if (gatewayUrl != null) {
            convivaConfig.setGatewayUrl(gatewayUrl);
        }

        // Add optional parameters
        convivaConfig.setDebugLoggingEnabled(true);

        // Create ConvivaAnalytics
        convivaAnalyticsIntegration = new ConvivaAnalyticsIntegration(
                bitmovinPlayer,
                customerKey,
                getApplicationContext(),
                convivaConfig);

        MetadataOverrides metadata = new MetadataOverrides();
        metadata.setApplicationName("Bitmovin Android Conviva integration example app");
        metadata.setViewerId("awesomeViewerId");

        Map<String, Object> standardTags = new HashMap<>();
        standardTags.put("c3.cm.contentType", "VOD");
        metadata.setAdditionalStandardTags(standardTags);

        Map<String, String> customTags = new HashMap<>();
        customTags.put("custom_tag", "Episode");
        metadata.setCustom(customTags);

        metadata.setImaSdkVersion("3.31.0");

        convivaAnalyticsIntegration.updateContentMetadata(metadata);

        // load source using the created source configuration
        bitmovinPlayer.load(buildSourceConfiguration());
    }

    private PlayerConfig buildPlayerConfiguration() {
        PlayerConfig playerConfiguration = new PlayerConfig();

        if (includeAdsSwitch.isChecked()) {
            playerConfiguration.setAdvertisingConfig(buildAdConfiguration());
        }

        return playerConfiguration;
    }

    private SourceConfig buildSourceConfiguration() {
        SourceConfig sourceConfig = new SourceConfig("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd", SourceType.Dash);
        sourceConfig.setTitle("Art of motion");
        return sourceConfig;
    }

    private AdvertisingConfig buildAdConfiguration() {
        // These are IMA Sample Tags from https://developers.google.com/interactive-media-ads/docs/sdks/android/tags

        String AD_SOURCE_1 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpostpod&cmsid=496&vid=short_onecue&correlator=";

        // Create AdSources
        AdSource firstAdSource = new AdSource(AdSourceType.Ima, AD_SOURCE_1);

        // Setup a pre-roll ad
        AdItem preRoll = new AdItem("pre", firstAdSource);

        // Add the AdItems to the AdvertisingConfiguration
        return new AdvertisingConfig(preRoll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bitmovinPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        bitmovinPlayerView.onStop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bitmovinPlayerView.onDestroy();
        convivaAnalyticsIntegration.release();
        super.onDestroy();
    }

    private void tearDownPlayer() {
        convivaAnalyticsIntegration.release();
        ViewGroup parent = (ViewGroup) bitmovinPlayerView.getParent();
        if (parent != null) {
            parent.removeView(bitmovinPlayerView);
        }
        bitmovinPlayer.destroy();
        bitmovinPlayerView.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton) {
            tearDownPlayer();
        } else if (v == createButton) {
            this.setupBitmovinPlayer();
        } else if (v == sendCustomEventButton) {
            Map<String, Object> eventAttributes = new HashMap<>();
            eventAttributes.put("Some", "Attributes");
            this.convivaAnalyticsIntegration.sendCustomPlaybackEvent("Custom Event", eventAttributes);
        } else if (v == pauseTrackingButton) {
            this.convivaAnalyticsIntegration.pauseTracking(false);
        } else if (v == resumeTrackingButton) {
            this.convivaAnalyticsIntegration.resumeTracking();
        }
    }
}
