package com.huan.capture;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

public class WebviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 WebView
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        setContentView(webView);

        // 获取传入的 URL
        String url = getIntent().getStringExtra("url");
        if (url != null) {
            // 设置 WebViewClient 以防止调用系统浏览器
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(url);
        } else {
            finish(); // 如果没有 URL，直接关闭页面
        }
    }
}
