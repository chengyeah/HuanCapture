package com.huan.capture;

import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * <br>
 *
 * <br>
 */
public class PushVideoActivity extends BaseWebRTCActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestScreenCapture();
    }
}
