package com.bitmovin.analytics.conviva.testapp.framework

import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType

object Sources {
    object Ads {
        const val VMAP_PREROLL_SINGLE_TAG = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator="

        const val VMAP_PREROLL_MIDROLL_POSTROLL_TAG = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="

        const val VAST_SINGLE_LINEAR_INLINE = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    }

    object Dash {
        val basic = SourceConfig(
                url = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd",
                type = SourceType.Dash,
                title = "Art of Motion Test Stream",
        )

        val basicLive = SourceConfig(
                url = "https://livesim.dashif.org/livesim2/testpic_2s/Manifest.mpd",
                type = SourceType.Dash,
                title = "DASH livesim Live Stream",
        )
    }

    object Hls {
        val basic = SourceConfig(
            url = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8",
            type = SourceType.Hls,
            title = "Art of Motion Test Stream",
        )
    }
}
