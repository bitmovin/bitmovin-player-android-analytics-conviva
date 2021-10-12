package com.bitmovin.analytics.conviva.testapp;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.bitmovin.analytics.conviva.ConvivaAnalytics;
import com.bitmovin.analytics.conviva.ConvivaAnalyticsException;
import com.bitmovin.analytics.conviva.ConvivaConfiguration;
import com.bitmovin.analytics.conviva.MetadataOverrides;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.advertising.AdItem;
import com.bitmovin.player.config.advertising.AdSource;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.config.advertising.AdvertisingConfiguration;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.PlaybackConfiguration;

public class MainActivity extends AppCompatActivity
{
    public static final String AUTOPLAY_KEY = "autoplay";
    public static final String VMAP_KEY = "vmapTag";
    public static final String SOURCE_KEY = "source";

    public BitmovinPlayerView bitmovinPlayerView;
    public SourceConfiguration bitmovinSourceConfiguration;
    public PlaybackConfiguration bitmovinPlaybackConfiguration;
    public AdvertisingConfiguration bitmovinAdConfiguration;
    public PlayerConfiguration bitmovinPlayerConfiguration;
    public BitmovinPlayer bitmovinPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        Boolean autoPlay = intent.getBooleanExtra(MainActivity.AUTOPLAY_KEY, false);
        String vmapTagUrl = intent.getStringExtra(MainActivity.VMAP_KEY);
        String sourceUrl = intent.getStringExtra(MainActivity.SOURCE_KEY);

        // Create a new source configuration
        String defaultSourceUrl = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd";
        this.bitmovinSourceConfiguration = new SourceConfiguration();
        // Add a new source item
        if (sourceUrl == null) {
            this.bitmovinSourceConfiguration.addSourceItem(defaultSourceUrl);
        } else {
            this.bitmovinSourceConfiguration.addSourceItem(sourceUrl);
        }

        this.bitmovinPlaybackConfiguration = new PlaybackConfiguration();
        this.bitmovinPlaybackConfiguration.setAutoplayEnabled(autoPlay);

        // Creating a new PlayerConfiguration
        this.bitmovinPlayerConfiguration = new PlayerConfiguration();
        // Assign created SourceConfiguration to the PlayerConfiguration
        this.bitmovinPlayerConfiguration.setSourceConfiguration(bitmovinSourceConfiguration);
        this.bitmovinPlayerConfiguration.setPlaybackConfiguration(bitmovinPlaybackConfiguration);


        // Create AdSources
        if (vmapTagUrl != null) {
            AdSource vmapAdSource = new AdSource(AdSourceType.IMA, vmapTagUrl);

            // Setup ad
            AdItem vmapAdRoll = new AdItem("", vmapAdSource);

            // Add the AdItems to the AdvertisingConfiguration
            this.bitmovinAdConfiguration = new AdvertisingConfiguration(vmapAdRoll);
            // Assing the AdvertisingConfiguration to the PlayerConfiguration
            // All ads in the AdvertisingConfiguration will be scheduled automatically
            this.bitmovinPlayerConfiguration.setAdvertisingConfiguration(this.bitmovinAdConfiguration);
        }

        // Create new BitmovinPlayerView with our PlayerConfiguration
        this.bitmovinPlayerView = new BitmovinPlayerView(this, bitmovinPlayerConfiguration);
        this.bitmovinPlayerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout rootView = (LinearLayout) this.findViewById(R.id.activity_main);

        // Add BitmovinPlayerView to the layout
        rootView.addView(this.bitmovinPlayerView, 0);

        this.bitmovinPlayer = bitmovinPlayerView.getPlayer();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        this.bitmovinPlayerView.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.bitmovinPlayerView.onResume();
    }

    @Override
    protected void onPause()
    {
        this.bitmovinPlayerView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop()
    {
        this.bitmovinPlayerView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        this.bitmovinPlayerView.onDestroy();
        super.onDestroy();
    }
}
