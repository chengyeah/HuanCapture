package com.huan.capture.webview;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.uc.webview.base.UCKnownException;
import com.uc.webview.export.extension.GlobalSettings;
import com.uc.webview.export.extension.IRunningCoreInfo;
import com.uc.webview.export.extension.IUrlDownloader;
import com.uc.webview.export.extension.SettingKeys;
import com.uc.webview.export.extension.U4Engine;
import com.uc.webview.export.extension.UCPlayer;

import java.io.File;

public class App extends Application {
    private String authKey = "Zd3SKoKMMocnOBsZGFB/toFxBncyb52nOUojSSGxt7xGnQKKG6IjDpoHy+3bl1KhLTHwkyXhLVAIspTtJEpipw==";
    //32
//    private String u4Url = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/03/libkernelu4_zip_uc_arm32.so";
//    private String u4PlayerUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/04/ApolloSo_player_32.zip";

    //64
    private String u4Url = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/03/libkernelu4_zip_uc_arm64.so";
    private String u4PlayerUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/01/ApolloSo_player.zip";

    @Override
    public void onCreate() {
        super.onCreate();
        initU4();
        initCloudModel();
    }

    private void initU4() {
        U4Engine.enableLog(true);
        GlobalSettings
                // 是否海外版本
                .set(SettingKeys.IsInternationalVersion, false)
                // 当前 App 版本信息
                .set(SettingKeys.UBISiVersion, "1.0")
                // 是否使用硬件加速
                .set(SettingKeys.IsHardwareAC, true)
                // 视频运行在标准渲染模式
                .set(SettingKeys.VideoUseStandardMode, true)
                // 是否使用阿波罗播放器，开启这个后，阿波罗播放器初始化功能才能生效
                .set(SettingKeys.SdkUseUCPlayer, true)
                // 初始化失败后是否使用系统 WebView 替代
                .set(SettingKeys.SdkInitFailedAndFallbackSystem, true)
                // 创建 WebView 等待内核初始化最大耗时，超时还未完成，则抛异常或者返回系统 WebView
                // 负数为一直等待至初始化结束，默认值为一直等待
                .set(SettingKeys.SdkInitWebViewMaxWaitMillis, 10000)
                // 是否允许复用旧内核
                .set(SettingKeys.SdkEnableReuseLastCore, true)
                // 是否开启 so 损坏检测，建议仅主进程开启
                .set(SettingKeys.SdkEnableCorruptionDetector, false)
                // 多进程私有数据目录后缀，如果 WebView 会运行在多个进程中，则必需要进行设置，
                // 用于隔离多个进程之间的数据
                .set(SettingKeys.PrivateDataDirSuffix, "1")
                // Render 进程模式，默认开启普通的 Render 进程模式
                .set(SettingKeys.RenderProcMode, SettingKeys.WEBVIEW_MULTI_PROCESS_NORMAL)
                // GPU 进程模式，默认开启独立 GPU 进程模式
                .set(SettingKeys.GpuProcMode, SettingKeys.GPU_PROCESS_FULL);

    }

    private void initU4Player() {
        Log.i("--==>", "initU4Player");
//        String downloadUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/04/ApolloSo_player_32.zip";
//        String downloadUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/01/ApolloSo_player.zip";
        UCPlayer.createUpdater()
                // 首先设置 ApplicationContext
                .setContext(getApplicationContext())
                // 设置下载链接，只支持 zip 格式
                .setUrl(u4PlayerUrl)
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
                        UCPlayer.setLibPath(dirPath);

                    }

                    @Override
                    public void onFailed(UCKnownException ex) {
                        // 初始化失败
                        Log.i("--==>", "player onFailed is :", ex);
                    }
                })
                // 开始异步初始化，如果配置参数不正确的话，该调用会直接抛出 UCKnownException
                .start();
    }

    private void initCloudModel() {
//        String downloadUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/06/30/libkernelu4_zip_uc.so";

//        String downloadUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/03/libkernelu4_zip_uc_arm32.so";
//        String downloadUrl = "https://extcdn.hsrc.tv/extend_screen/files/plugin/2025/07/03/libkernelu4_zip_uc_arm64.so";
        U4Engine.createInitializer()
                // 首先设置 ApplicationContext
                .setContext(getApplicationContext())
                // 设置授权码
                .setAuthKey(authKey)
                // 设置下载链接，只支持 zip 格式
                .setUrl(u4Url)
                // 监听初始化状态的回调
                .setClient(new U4Engine.InitializerClient() {
                    @Override
                    public void onInitStart(IRunningCoreInfo info) {
                        // 准备开始初始化
                        Log.i("--==>", "onInitStart: " + info);
                    }

                    @Override
                    public boolean onDownloadStart(
                            String url, U4Engine.IDownloadHandle handle) {
                        Log.i("--==>", "onDownloadStart: " + url);
                        // 准备开始下载，如果本地文件已经下载完成，则无该回调
                        // 1. 返回 true，则表示可以继续进行下载
                        // 2. 返回 false，则表示下载中止
                        // 3. 下载中止后，可通过 handle 来恢复下载或者取消下载
                        // 4. 下载中止后，必须择机进行恢复或者取消，否则会有资源占用
                        return true;
                    }

                    @Override
                    public void onDownloadProgress(int progress) {
                        Log.i("--==>", "onDownloadProgress: " + progress);
                        // 下载进度，进度增加 20% 或以上，则回调一次，
                        // 如果是自定义下载器，则以自定义下载器的返回进度为主
                    }

                    @Override
                    public void onDownloadFinish(String url, File savedFile) {
                        Log.i("--==>", "onDownloadFinish: " + url);
                        // 下载完成
                    }

                    @Override
                    public boolean onExtractStart(File compressedFile, File outDir) {
                        Log.i("--==>", "onExtractStart: " + compressedFile);
                        // 解压开始，如果无需解压情况，则无该回调
                        // 1. 返回true，则表示可以继续进行内部解压逻辑
                        // 2. 如果需要自定义解压，则在该函数解压完成后，并返回false
                        // 3. 如果抛出异常，则终止后续初始化流程
                        return true;
                    }

                    @Override
                    public void onExtractFinish(File dir) {
                        Log.i("--==>", "onExtractFinish: " + dir);
                        // 解压结束，如果无需解压情况，则无该回调
                    }

                    @Override
                    public void onDexReady(ClassLoader loader) {
                        Log.i("--==>", "onDexReady: " + loader);
                        // U4 内核使用的 core.jar 加载完成
                    }

                    @Override
                    public void onNativeReady(File libDir) {
                        Log.i("--==>", "onNativeReady: " + libDir);
                        // U4 内核 Native 环境初始化完成，主要指 libwebviewuc.so 加载成功
                    }

                    @Override
                    public void onSuccess(IRunningCoreInfo info) {
                        Log.i("--==>", "onSuccess: " + info);
                        // 初始化流程完成，且成功，可开始使用 U4 WebView
                        initU4Player();
                    }

                    @Override
                    public void onFailed(IRunningCoreInfo info) {
                        Log.i("--==>", "onFailed: " + info);
                        // 初始化失败，可通过 info.failedInfo() 获取异常信息
                    }
                })
                // 开始异步初始化，如果配置参数不正确的话，该调用会直接抛出 UCKnownException
                .start();
    }


}
