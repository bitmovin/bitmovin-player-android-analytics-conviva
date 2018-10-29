package com.bitmovin.analytics.convivaanalyticsexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bitmovin.analytics.conviva.ConvivaAnalytics;
import com.bitmovin.analytics.conviva.ConvivaConfiguration;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.media.DASHSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // UI
    private Button releaseButton;
    private Button createButton;

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

        this.bitmovinPlayerView = this.findViewById(R.id.bitmovinPlayerView);
        this.bitmovinPlayer = this.bitmovinPlayerView.getPlayer();

        this.initializePlayer();
    }

    protected void initializePlayer() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        // Add a new source item
        DASHSource dashSource = new DASHSource("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");
        SourceItem sourceItem = new SourceItem(dashSource);
        sourceConfiguration.addSourceItem(sourceItem);

        // Create your ConvivaConfiguration object
        ConvivaConfiguration convivaConfig = new ConvivaConfiguration(
                "ConvivaExample_BitmovinPlayer",
                "ViewerId1");

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
        bitmovinPlayer.load(sourceConfiguration);
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
            convivaAnalytics.detachPlayer();
            bitmovinPlayer.unload();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == releaseButton) {
            releasePlayer();
        } else if (v == createButton) {
            initializePlayer();
        }
    }
}
