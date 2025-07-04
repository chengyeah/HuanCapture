package com.huan.capture.webview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.huan.capture.R;
import com.uc.webview.base.UCKnownException;

import com.uc.webview.export.WebSettings;
import com.uc.webview.export.WebView;
import com.uc.webview.export.extension.U4Engine;
import com.uc.webview.export.extension.UCPlayer;

import java.io.File;

public class WebViewActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        WebView webView = findViewById(R.id.webView);
        Button btnOpen = findViewById(R.id.btnOpen);
        btnOpen.requestFocus();
        Button btnDownLoadPlayer = findViewById(R.id.btnDownLoadPlayer);

        btnDownLoadPlayer.setOnClickListener(view -> {

        });

        btnOpen.setOnClickListener(view -> {
//            webView.loadUrl("https://www.yangshipin.cn/tv/home?pid=600002475");
            webView.loadUrl("https://www.miguvideo.com/p/live/120000541524");
//            webView.loadUrl("https://tv.cctv.com/live/cctv5/");
        });

        webView.getSettings().setUserAgentString(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
        );
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true); // 文件访问
        settings.setDomStorageEnabled(true); // 启用 DOM Storage（有些播放器用 localStorage）
        settings.setDatabaseEnabled(true); // 启用 Web SQL（老播放器用）
//        锦绣芳华
        settings.setUseWideViewPort(true);

        settings.setSupportZoom(true);
        int coreType = WebView.getCoreType();
        boolean usingU4 = isU4(coreType);
        Toast.makeText(this, "当前使用的内核是：" + (usingU4 ? "U4" : "android"), Toast.LENGTH_SHORT).show();
    }

    public boolean isU4(int coreType) {
        return WebView.CORE_TYPE_U4 == coreType;
    }
}
