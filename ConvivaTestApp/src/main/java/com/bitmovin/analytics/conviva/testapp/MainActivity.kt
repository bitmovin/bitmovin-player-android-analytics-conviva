package com.bitmovin.analytics.conviva.testapp

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.player.PlayerView
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig

class MainActivity : AppCompatActivity() {
    lateinit var bitmovinPlayerView: PlayerView
    lateinit var bitmovinPlaybackConfig: PlaybackConfig
    lateinit var bitmovinPlayerConfig: PlayerConfig
    lateinit var bitmovinPlayer: Player
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the Intent that started this activity and extract the string
        val intent = intent
        val autoPlay = intent.getBooleanExtra(AUTOPLAY_KEY, false)
        var vmapTagUrl = intent.getStringExtra(VMAP_KEY)
        bitmovinPlaybackConfig = PlaybackConfig().apply {
            isAutoplayEnabled = autoPlay
        }

        // Creating a new PlayerConfiguration
        bitmovinPlayerConfig = PlayerConfig().apply {
            playbackConfig = bitmovinPlaybackConfig
        }

        // Create Ad configuration and adding to player configuration
        if (vmapTagUrl != null) {
            // Setup ad
            val vmapAdSource = AdSource(AdSourceType.Ima, vmapTagUrl)
            val vmapAdRoll = AdItem("", vmapAdSource)

            // Add the AdItems to the AdvertisingConfiguration
            bitmovinPlayerConfig.advertisingConfig = AdvertisingConfig(vmapAdRoll)
        }

        // Create new BitmovinPlayerView with our PlayerConfiguration
        bitmovinPlayer = Player.create(this, bitmovinPlayerConfig!!)
        bitmovinPlayerView = PlayerView(this, bitmovinPlayer).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        val rootView = findViewById<View>(R.id.activity_main) as LinearLayout

        // Add BitmovinPlayerView to the layout
        rootView.addView(bitmovinPlayerView, 0)
        bitmovinPlayer = bitmovinPlayerView?.player!!
    }

    override fun onStart() {
        super.onStart()
        bitmovinPlayerView.onStart()
    }

    override fun onResume() {
        super.onResume()
        bitmovinPlayerView.onResume()
    }

    override fun onPause() {
        bitmovinPlayerView.onPause()
        super.onPause()
    }

    override fun onStop() {
        bitmovinPlayerView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        bitmovinPlayerView.onDestroy()
        super.onDestroy()
    }

    companion object {
        const val AUTOPLAY_KEY = "autoplay"
        const val VMAP_KEY = "vmapTag"
    }
}
