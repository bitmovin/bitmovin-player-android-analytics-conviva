package com.bitmovin.analytics.conviva.helper;

import com.bitmovin.player.api.event.Event;
import com.bitmovin.player.api.event.JavaEventEmitter;

public interface WithEventEmitter {
    void call(JavaEventEmitter<Event> player);
}