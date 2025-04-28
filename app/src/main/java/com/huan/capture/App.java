package com.huan.capture;

import android.app.Application;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.IEsMessenger;
import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.bean.EsEvent;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        EsMessenger.get().setMessengerCallback(new IEsMessenger.MessengerCallback() {
            @Override
            public void onPingResponse(String s, int i) {

            }

            @Override
            public void onFindDevice(EsDevice esDevice) {
                ConfigParams.getInstance().sendDeviceInfo(esDevice);
            }

            @Override
            public void onReceiveEvent(EsEvent esEvent) {
                ConfigParams.getInstance().sendMessage(esEvent);
            }
        });
    }
}
