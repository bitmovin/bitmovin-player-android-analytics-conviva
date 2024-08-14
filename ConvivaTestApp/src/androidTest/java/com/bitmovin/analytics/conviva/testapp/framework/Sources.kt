package com.bitmovin.analytics.conviva.testapp.framework

import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType

object Sources {
    object Ads {
        const val VMAP_PREROLL_SINGLE_TAG = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator="
    }

    object Dash {
        val basic = SourceConfig(
                url = "https://bitmovin-a.akamaihd.net/content/MI201109210084_1/mpds/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.mpd",
                type = SourceType.Dash,
                title = "Art of Motion Test Stream",
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
