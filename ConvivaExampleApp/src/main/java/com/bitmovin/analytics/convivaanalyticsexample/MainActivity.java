package com.bitmovin.analytics.convivaanalyticsexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.bitmovin.analytics.conviva.ConvivaAnalytics;
import com.bitmovin.analytics.conviva.ConvivaConfig;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.config.media.SourceConfiguration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinPlayer bitmovinPlayer;
    private ConvivaAnalytics convivaAnalytics;
    private Button releaseButton;
    private Button createButton;

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
        this.bitmovinPlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);

        this.initializePlayer();

    }


    protected void initializePlayer() {
        // Create a new source configuration
        SourceConfiguration sourceConfiguration = new SourceConfiguration();

        // Add a new source item
        sourceConfiguration.addSourceItem("https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd");

        // Create your ConvivaConfig object
        ConvivaConfig convivaConfig = new ConvivaConfig("e94c66c4c6eb1d888077767e5db0d7b12b15f5b6", "https://rtl-nl-xl-test.testonly.conviva.com/","ConvivaExample_BitmovinPlayer","ViewerId1","Asset1");

        // Add optional parameters
        convivaConfig.setDebugLoggingEnabled(true);

        // Create ConvivaAnalytics
        convivaAnalytics = ConvivaAnalytics.getInstance();
        convivaAnalytics.attachPlayer(convivaConfig, bitmovinPlayer, getApplicationContext());

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
