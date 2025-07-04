package com.huan.capture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class XYWebSocketClientManager {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private WebSocketClient mWebSocketClient;
    private final Context mContext;

    public XYWebSocketClientManager(Context context) {
        mContext = context;
    }

    /**
     * 连接到 WebSocket 服务器
     *
     * @param serverUri
     */
    public void connectToServer(String serverUri) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            return;
        }
        URI uri;
        try {
            uri = new URI(serverUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                showToast("WebSocket 客户端连接成功");
            }

            @Override
            public void onMessage(String message) {
                Log.i("--==>", "客户端收到socket的消息 is：" + message);

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                showToast("WebSocket 客户端断开: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        mWebSocketClient.setConnectionLostTimeout(3000);
        mWebSocketClient.connect();
    }

    /**
     * 断开 WebSocket 连接
     */
    public void disconnect() {
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
            mWebSocketClient = null;
        }
    }

    /**
     * 发送消息到服务器
     *
     * @param message
     */
    public void sendToServer(String message) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            mWebSocketClient.send(message);
        }
    }

    private void showToast(String message) {
        mHandler.post(() -> {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        });
    }
}
