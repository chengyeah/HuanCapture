package com.huan.capture.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Iterator;
import java.util.List;

public class AuxiliaryService extends AccessibilityService {

    public static AuxiliaryService mService;
    private ClickReceiver receiver;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mService = this;
        //连接服务后,一般是在授权成功后会接收到
        if (receiver == null) {
            receiver = new ClickReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("auto.click");
            registerReceiver(receiver, intentFilter);
        }
    }

    /**
     * 通过系统监听窗口变化的回调,sendAccessibiliyEvent()不断的发送AccessibilityEvent到此处
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        List<AccessibilityNodeInfo> installNodes = rootNode.findAccessibilityNodeInfosByText("安装");
        Log.i("--==>", "----------->installNodes.size():" + installNodes.size());
        for (AccessibilityNodeInfo node : installNodes) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    /**
     * 查找拥有特定焦点类型的控件
     */
    @Override
    public AccessibilityNodeInfo findFocus(int focus) {
        return super.findFocus(focus);
    }

    /**
     * 如果配置能够获取窗口内容,则会返回当前活动窗口的根结点
     */
    @Override
    public AccessibilityNodeInfo getRootInActiveWindow() {
        return super.getRootInActiveWindow();
    }


    /**
     * 获取系统服务
     */
    @Override
    public Object getSystemService(String name) {
        return super.getSystemService(name);
    }

    /**
     * 如果允许服务监听按键操作，该方法是按键事件的回调，需要注意，这个过程发生了系统处理按键事件之前
     */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return super.onKeyEvent(event);
    }

    /**
     * 中断服务时的回调
     */
    @Override
    public void onInterrupt() {
        mService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService = null;
    }

    /**
     * 辅助功能是否启动
     */
    public static boolean isStart() {
        return mService != null;
    }

    /**
     * 判断当前服务是否正在运行
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if (mService == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) mService.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = mService.getServiceInfo();
        if (info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if (i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if (!isConnect) {
            return false;
        }
        return true;
    }

    //执行点击
    private void performClick() {
        Log.i("--==>", "----------->执行点击");
    }

    class ClickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            performClick();
        }
    }
}
