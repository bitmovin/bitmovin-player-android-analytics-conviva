package com.bitmovin.analytics.conviva;

import com.conviva.api.player.IClientMeasureInterface;

public class ClientMeasureInterface implements IClientMeasureInterface {

    private float frameRate;

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
    }

    // IClientMeasureInterface

    @Override
    public int getFrameRate() {
        return Math.round(frameRate);
    }

    @Override
    public long getPHT() {
        return 0;
    }

    @Override
    public int getBufferLength() {
        return 0;
    }

    @Override
    public double getSignalStrength() {
        return 0;
    }

    @Override
    public void getCDNServerIP() {

    }
}
