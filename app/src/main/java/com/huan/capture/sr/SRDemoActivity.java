package com.huan.capture.sr;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.huan.capture.R;
import com.huan.capture.sr.webrtc.capture.CameraVideoCaptureAdapter;
import com.huan.capture.sr.webrtc.capture.LocalYuvVideoCaptureAdapter;
import com.huan.capture.sr.webrtc.capture.ScreenVideoCaptureAdapter;
import com.huan.capture.sr.webrtc.transfer.TransferAdapterEsMessenger;
import com.huan.capture.sr.webrtc.capture.VideoCaptureAdapter;
import com.huan.capture.sr.webrtc.WebRTCManager;

import org.webrtc.VideoTrack;

/**
 * <br>
 *
 * <br>
 */
public class SRDemoActivity extends AppCompatActivity {


    private WebRTCManager mWebRTCManager;
    private VideoCaptureAdapter mVideoCaptureAdapter;

    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 2;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_srdemo);

        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            Log.d("--==>", "解码器: " + codecInfo.getName());
        }
    }

    public void onButtonClicked(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.btn_search_device:
                if (mWebRTCManager != null) {
                    mWebRTCManager.release();
                }
                mWebRTCManager = new WebRTCManager();
                mWebRTCManager.init(this, new TransferAdapterEsMessenger());
                break;
            case R.id.btn_push_camera:
                if (allPermissionsGranted()) {
                    startCameraTack();
                } else {
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                }
                break;
            case R.id.btn_switch_camera:
                if (mVideoCaptureAdapter instanceof CameraVideoCaptureAdapter) {
                    mWebRTCManager.stopCurrentVideoCapture(false);
                    ((CameraVideoCaptureAdapter) mVideoCaptureAdapter).switchCamera();
                    mWebRTCManager.replaceVideoCapture(mVideoCaptureAdapter);
                }
                break;
            case R.id.btn_push_screen:
                requestScreenCapture();
                break;
            case R.id.btn_push_yuv_video:
                mWebRTCManager.stopCurrentVideoCapture(true);
                LocalYuvVideoCaptureAdapter captureAdapter = new LocalYuvVideoCaptureAdapter(this);
                setOrReplaceVideoTrack(captureAdapter);
                break;
            case R.id.btn_stop:
                release();
                break;
        }
    }

    //region 摄像头

    private void startCameraTack() {
        if (mVideoCaptureAdapter instanceof CameraVideoCaptureAdapter) {
            return;
        }

        mWebRTCManager.stopCurrentVideoCapture(true);

        VideoCaptureAdapter capture = new CameraVideoCaptureAdapter();
        setOrReplaceVideoTrack(capture);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraTack();
            } else {
                Toast.makeText(this, "权限被拒绝，无法继续操作", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //endregion

    //region 录屏

    private void requestScreenCapture() {
        if (mVideoCaptureAdapter instanceof ScreenVideoCaptureAdapter) {
            return;
        }
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            mWebRTCManager.stopCurrentVideoCapture(true);

            ScreenVideoCaptureAdapter capture = new ScreenVideoCaptureAdapter();
            capture.startScreenCapture(this, data, () -> {
                setOrReplaceVideoTrack(capture);
            });
            mVideoCaptureAdapter = capture;
        }
    }

    //endregion

    private void setOrReplaceVideoTrack(VideoCaptureAdapter capture) {
        if (mWebRTCManager.isVideoTrackerExist()) {
            mWebRTCManager.replaceVideoCapture(capture);
        } else {
            VideoTrack videoTrack = mWebRTCManager.createVideoTrack(capture);
//            mWebRTCManager.addVideoTrack(videoTrack, findViewById(R.id.localView));
            mWebRTCManager.addVideoTrack(videoTrack);
        }

        mVideoCaptureAdapter = capture;
    }

    private void release() {
        if (mWebRTCManager != null) {
            mWebRTCManager.stopCurrentVideoCapture(true);
            mWebRTCManager.release();
        }
        mVideoCaptureAdapter = null;
    }

    private void destroy() {
        mWebRTCManager.destroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
        destroy();
    }
}
