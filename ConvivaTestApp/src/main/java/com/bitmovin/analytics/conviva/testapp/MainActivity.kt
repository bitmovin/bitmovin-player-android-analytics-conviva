package com.bitmovin.analytics.conviva.testapp

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.BitmovinPlayerView
import com.bitmovin.player.config.PlaybackConfiguration
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.advertising.AdItem
import com.bitmovin.player.config.advertising.AdSource
import com.bitmovin.player.config.advertising.AdSourceType
import com.bitmovin.player.config.advertising.AdvertisingConfiguration
import com.bitmovin.player.config.media.SourceConfiguration

class MainActivity : AppCompatActivity() {
    var bitmovinPlayerView: BitmovinPlayerView? = null
    var bitmovinSourceConfiguration: SourceConfiguration? = null
    var bitmovinPlaybackConfiguration: PlaybackConfiguration? = null
    var bitmovinAdConfiguration: AdvertisingConfiguration? = null
    var bitmovinPlayerConfiguration: PlayerConfiguration? = null
    var bitmovinPlayer: BitmovinPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the Intent that started this activity and extract the string
        val intent = intent
        val autoPlay = intent.getBooleanExtra(AUTOPLAY_KEY, false)
        var vmapTagUrl = intent.getStringExtra(VMAP_KEY)
        bitmovinPlaybackConfiguration = PlaybackConfiguration()
        bitmovinPlaybackConfiguration!!.isAutoplayEnabled = autoPlay

        // Creating a new PlayerConfiguration
        bitmovinPlayerConfiguration = PlayerConfiguration()
        // Assign created SourceConfiguration to the PlayerConfiguration
        bitmovinPlayerConfiguration!!.sourceConfiguration = bitmovinSourceConfiguration
        bitmovinPlayerConfiguration!!.playbackConfiguration = bitmovinPlaybackConfiguration

        // Create AdSources
        if (vmapTagUrl != null) {
            val vmapAdSource = AdSource(AdSourceType.IMA, vmapTagUrl)

            // Setup ad
            val vmapAdRoll = AdItem("", vmapAdSource)

            // Add the AdItems to the AdvertisingConfiguration
            bitmovinAdConfiguration = AdvertisingConfiguration(vmapAdRoll)
            // Assing the AdvertisingConfiguration to the PlayerConfiguration
            // All ads in the AdvertisingConfiguration will be scheduled automatically
            bitmovinPlayerConfiguration!!.advertisingConfiguration = bitmovinAdConfiguration
        }

        // Create new BitmovinPlayerView with our PlayerConfiguration
        bitmovinPlayerView = BitmovinPlayerView(this, bitmovinPlayerConfiguration)
        bitmovinPlayerView!!.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val rootView = findViewById<View>(R.id.activity_main) as LinearLayout

        // Add BitmovinPlayerView to the layout
        rootView.addView(bitmovinPlayerView, 0)
        bitmovinPlayer = bitmovinPlayerView!!.player
    }

    override fun onStart() {
        super.onStart()
        bitmovinPlayerView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        bitmovinPlayerView!!.onResume()
    }

    override fun onPause() {
        bitmovinPlayerView!!.onPause()
        super.onPause()
    }

    override fun onStop() {
        bitmovinPlayerView!!.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        bitmovinPlayerView!!.onDestroy()
        super.onDestroy()
    }

    companion object {
        const val AUTOPLAY_KEY = "autoplay"
        const val VMAP_KEY = "vmapTag"
    }
}