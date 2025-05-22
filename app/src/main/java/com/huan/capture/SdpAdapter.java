package com.huan.capture;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * Created by chao on 2019/1/29.
 */

public class SdpAdapter implements SdpObserver {
    private OnSdpAdapterListener mListener;
    private String tag;

    public SdpAdapter(String tag) {
        this.tag = "chao " + tag;
    }

    public SdpAdapter(String tag, OnSdpAdapterListener listener) {
        this.mListener = listener;
        this.tag = tag;
    }

    private void log(String s) {
        Log.d(tag, s);
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        log("onCreateSuccess " + sessionDescription);
    }

    @Override
    public void onSetSuccess() {
        log("onSetSuccess ");
        if (mListener != null) {
            mListener.onSetSuccess();
        }
    }

    @Override
    public void onCreateFailure(String s) {
        log("onCreateFailure " + s);
    }

    @Override
    public void onSetFailure(String s) {
        log("onSetFailure " + s);
    }

    public interface OnSdpAdapterListener {
        void onSetSuccess();
    }
}
