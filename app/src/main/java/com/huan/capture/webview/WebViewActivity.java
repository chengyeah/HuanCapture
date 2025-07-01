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
import com.uc.webview.export.extension.GlobalSettings;
import com.uc.webview.export.extension.SettingKeys;
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
        Button btnDownLoadPlayer = findViewById(R.id.btnDownLoadPlayer);

        btnDownLoadPlayer.setOnClickListener(view -> {
            initU4Player();
        });

        btnOpen.setOnClickListener(view -> {
            webView.loadUrl("https://tv.cctv.com/live/cctv5/");
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

        settings.setUseWideViewPort(true);

        settings.setSupportZoom(true);
        int coreType = WebView.getCoreType();
        boolean usingU4 = isU4(coreType);
        Toast.makeText(this, "当前使用的内核是：" + (usingU4 ? "U4" : "android"), Toast.LENGTH_SHORT).show();
    }

    public boolean isU4(int coreType) {
        return WebView.CORE_TYPE_U4 == coreType;
    }

    private void initU4Player() {
        String downloadUrl = "http://192.168.7.118:8000/ApolloSo_player.zip";
        UCPlayer.createUpdater()
                // 首先设置 ApplicationContext
                .setContext(getApplicationContext())
                // 设置下载链接，只支持 zip 格式
                .setUrl(downloadUrl)
                // 监听初始化状态的回调
                .setClient(new UCPlayer.UpdaterClient() {
                    @Override
                    public boolean onDownloadStart(
                            String url, U4Engine.IDownloadHandle handle) {
                        // 准备开始下载，如果本地文件已经下载完成，则无该回调
                        // 1. 返回 true，则表示可以继续进行下载
                        // 2. 返回 false，则表示下载中止
                        // 3. 下载中止后，可通过 handle 来恢复下载或者取消下载
                        // 4. 下载中止后，必须择机进行恢复或者取消，否则会有资源占用

                        Log.i("--==>", "player onDownloadStart: " + url);

                        return true;
                    }

                    @Override
                    public void onDownloadProgress(int progress) {

                        Log.i("--==>", "player onDownloadProgress: " + progress);
                        // 下载进度，进度增加 20% 或以上，则回调一次，
                        // 如果是自定义下载器，则以自定义下载器的返回进度为主
                    }

                    @Override
                    public void onDownloadFinish(String url, File savedFile) {
                        // 下载完成
                        Log.i("--==>", "player onDownloadFinish: " + savedFile.getAbsolutePath());
                    }

                    @Override
                    public void onSuccess(String dirPath) {
                        // 初始化完成
                        Log.i("--==>", "player onSuccess is :" + dirPath);
                        // 将路径设置给内核，必须的。
//                        UCPlayer.setLibPath(dirPath);
                    }

                    @Override
                    public void onFailed(UCKnownException ex) {
                        // 初始化失败
                        Log.i("--==>", "player onFailed is :",ex);
                    }
                })
                // 开始异步初始化，如果配置参数不正确的话，该调用会直接抛出 UCKnownException
                .start();
    }
}
