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
    lateinit var bitmovinAdConfig: AdvertisingConfig
    lateinit var bitmovinPlayerConfig: PlayerConfig
    lateinit var bitmovinPlayer: Player
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the Intent that started this activity and extract the string
        val intent = intent
        val autoPlay = intent.getBooleanExtra(AUTOPLAY_KEY, false)
        var vmapTagUrl = intent.getStringExtra(VMAP_KEY)
        bitmovinPlaybackConfig = PlaybackConfig()
        bitmovinPlaybackConfig!!.isAutoplayEnabled = autoPlay

        // Creating a new PlayerConfiguration
        bitmovinPlayerConfig = PlayerConfig()
        bitmovinPlayerConfig!!.playbackConfig = bitmovinPlaybackConfig!!

        // Create AdSources
        if (vmapTagUrl != null) {
            val vmapAdSource = AdSource(AdSourceType.Ima, vmapTagUrl)

            // Setup ad
            val vmapAdRoll = AdItem("", vmapAdSource)

            // Add the AdItems to the AdvertisingConfiguration
            bitmovinAdConfig = AdvertisingConfig(vmapAdRoll)
            // Assing the AdvertisingConfiguration to the PlayerConfiguration
            // All ads in the AdvertisingConfiguration will be scheduled automatically
            bitmovinPlayerConfig!!.advertisingConfig = bitmovinAdConfig!!
        }

        // Create new BitmovinPlayerView with our PlayerConfiguration
        bitmovinPlayer = Player.create(this, bitmovinPlayerConfig!!)
        bitmovinPlayerView = PlayerView(this, bitmovinPlayer)
        bitmovinPlayerView!!.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
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
