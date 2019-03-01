package com.bitmovin.analytics.convivaanalyticsexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.bitmovin.analytics.conviva.ConvivaAnalytics;
import com.bitmovin.analytics.conviva.ConvivaConfiguration;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.media.DASHSource;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.advertising.AdItem;
import com.bitmovin.player.config.advertising.AdSource;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.config.advertising.AdvertisingConfiguration;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;

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
    private BitmovinPlayer bitmovinPlayer;
    private BitmovinPlayerView bitmovinPlayerView;

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
        this.bitmovinPlayer = new BitmovinPlayer(this);
        this.bitmovinPlayerView = new BitmovinPlayerView(this, this.bitmovinPlayer);

        LinearLayout playerUIView = this.findViewById(R.id.bitmovinPlayerUIView);
        playerUIView.addView(bitmovinPlayerView);

        // Create your ConvivaConfiguration object
        ConvivaConfiguration convivaConfig = new ConvivaConfiguration();

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

        // load source using the created source configuration
        bitmovinPlayer.setup(buildPlayerConfiguration());
    }

    private PlayerConfiguration buildPlayerConfiguration() {
        PlayerConfiguration playerConfiguration = new PlayerConfiguration();

        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();
        DASHSource source = new DASHSource("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");
        SourceItem sourceItem = new SourceItem(source);
        sourceItem.setTitle("Art of motion");

        // Add a new source item
        sourceConfiguration.addSourceItem(sourceItem);

        // Add sourceConfiguration to playerConfiguration
        playerConfiguration.setSourceConfiguration(sourceConfiguration);

        if (includeAdsSwitch.isChecked()) {
            playerConfiguration.setAdvertisingConfiguration(buildAdConfiguration());
        }

        return playerConfiguration;
    }

    private AdvertisingConfiguration buildAdConfiguration() {
        // These are IMA Sample Tags from https://developers.google.com/interactive-media-ads/docs/sdks/android/tags

        String AD_SOURCE_1 = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpostpod&cmsid=496&vid=short_onecue&correlator=";

        // Create AdSources
        AdSource firstAdSource = new AdSource(AdSourceType.IMA, AD_SOURCE_1);

        // Setup a pre-roll ad
        AdItem preRoll = new AdItem("pre", firstAdSource);

        // Add the AdItems to the AdvertisingConfiguration
        return new AdvertisingConfiguration(preRoll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bitmovinPlayerView.onResume();
    }

    @Override
    protected void onPause() {
        bitmovinPlayerView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bitmovinPlayerView.onDestroy();
        super.onDestroy();
    }

    private void releasePlayer() {
        if (bitmovinPlayer != null) {
            bitmovinPlayer.unload();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton) {
            releasePlayer();
        } else if (v == createButton) {
            bitmovinPlayer.setup(buildPlayerConfiguration());
        } else if (v == sendCustomEventButton) {
            Map<String, Object> eventAttributes = new HashMap<>();
            eventAttributes.put("Some", "Attributes");
            this.convivaAnalytics.sendCustomPlaybackEvent("Custom Event", eventAttributes);
        }
    }
}
