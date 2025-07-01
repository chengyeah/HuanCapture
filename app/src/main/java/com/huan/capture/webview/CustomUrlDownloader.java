package com.huan.capture.webview;

import android.util.Log;

import com.uc.webview.export.extension.IUrlDownloader;

public class CustomUrlDownloader implements IUrlDownloader {
    @Override
    public boolean start(String s, String s1, Client client) {
        Log.i("--==>", "start: " + s);
        return false;
    }

    @Override
    public void stop() {
        Log.i("--==>", "stop: ");
    }

    @Override
    public void delete() {
        Log.i("--==>", "delete: ");
    }
}
