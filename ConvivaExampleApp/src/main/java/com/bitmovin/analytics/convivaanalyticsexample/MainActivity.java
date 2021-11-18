package com.bitmovin.analytics.convivaanalyticsexample;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.bitmovin.analytics.conviva.ConvivaAnalytics;
import com.bitmovin.analytics.conviva.ConvivaAnalyticsException;
import com.bitmovin.analytics.conviva.ConvivaConfig;
import com.bitmovin.analytics.conviva.MetadataOverrides;
import com.bitmovin.player.api.Player;
import com.bitmovin.player.PlayerView;
import com.bitmovin.player.api.source.Source;
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
    private Button releaseButton;
    private Button createButton;
    private Button sendCustomEventButton;
    private Switch includeAdsSwitch;

    // Conviva
    private static final String customerKey = "";
    private static String gatewayUrl; // Set only in debug mode
    private ConvivaAnalytics convivaAnalytics;

    // Player
    private Player bitmovinPlayer;
    private PlayerView bitmovinPlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        this.bitmovinPlayer = Player.create(this, buildPlayerConfiguration());
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
        convivaAnalytics = new ConvivaAnalytics(
                bitmovinPlayer,
                customerKey,
                getApplicationContext(),
                convivaConfig);


        MetadataOverrides metadata = new MetadataOverrides();
        metadata.setApplicationName("Bitmovin Android Conviva integration example app");
        metadata.setViewerId("awesomeViewerId");
        Map<String, String> customInternTags = new HashMap<>();
        customInternTags.put("contentType", "Episode");
        metadata.setCustom(customInternTags);
        convivaAnalytics.updateContentMetadata(metadata);

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
        try {
            convivaAnalytics.initializeSession();
        } catch (ConvivaAnalyticsException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        bitmovinPlayerView.onStop();
        convivaAnalytics.endSession();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bitmovinPlayerView.onDestroy();
        super.onDestroy();
    }

    private void unloadPlayer() {
        if (bitmovinPlayer != null) {
            bitmovinPlayer.unload();
        }
    }

    private void loadPlayer(SourceConfig sourceConfig) {
        if (bitmovinPlayer != null) {
            bitmovinPlayer.load(sourceConfig);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton) {
            unloadPlayer();
        } else if (v == createButton) {
            loadPlayer(buildSourceConfiguration());
        } else if (v == sendCustomEventButton) {
            Map<String, Object> eventAttributes = new HashMap<>();
            eventAttributes.put("Some", "Attributes");
            this.convivaAnalytics.sendCustomPlaybackEvent("Custom Event", eventAttributes);
        }
    }
}

