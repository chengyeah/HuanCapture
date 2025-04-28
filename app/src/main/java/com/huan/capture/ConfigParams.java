package com.huan.capture;

import eskit.sdk.support.messenger.client.bean.EsDevice;
import eskit.sdk.support.messenger.client.bean.EsEvent;

public class ConfigParams {
    public static EsDevice mEsDevice = null;
    private OnClientMessageListener mListener;

    //单例
    public static ConfigParams getInstance() {
        return ConfigParamsHolder.instance;
    }

    private static class ConfigParamsHolder {
        private static final ConfigParams instance = new ConfigParams();
    }

    private ConfigParams() {
        //初始化
    }

    public void setOnClientMessageListener(OnClientMessageListener listener) {
        this.mListener = listener;
    }

    public void sendMessage(EsEvent esEvent) {
        if (mListener != null) {
            mListener.onMessage(esEvent);
        }
    }

    public void sendDeviceInfo(EsDevice esDevice){
        if (mListener != null) {
            mListener.onDeviceInfo(esDevice);
        }
    }

    public interface OnClientMessageListener {
        default void onMessage(EsEvent esEvent){}

        default void onDeviceInfo(EsDevice esDevice){}
    }
}
