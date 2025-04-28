package com.huan.capture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.InetSocketAddress;

public class WebSocketManager {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private WebSocketServer mWebSocketServer;
    private final Context mContext;
    private WebSocket currentConnection;
    private OnOfferReceivedListener onOfferReceived;
    private OnAnswerReceivedListener onAnswerReceived;
    private OnIceCandidateReceivedListener onIceCandidateReceived;

    public WebSocketManager(Context context) {
        mContext = context;
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param ipAddress
     * @param port
     */
    public void startServer(String ipAddress, int port) {
        if (mWebSocketServer != null && !mWebSocketServer.getConnections().isEmpty()) {
            return;
        }
        mWebSocketServer = new MyWebSocketServer(new InetSocketAddress(ipAddress, port));
        mWebSocketServer.start();
    }

    /**
     * 停止 WebSocket 服务器
     */
    public void stopServer() {
        if (mWebSocketServer != null) {
            try {
                if (currentConnection != null) {
                    currentConnection.close();
                }
                mWebSocketServer.stop(1000);
                mWebSocketServer = null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // WebSocket 服务器类，用来处理连接和消息
    private class MyWebSocketServer extends WebSocketServer {

        public MyWebSocketServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            showToast("连接: " + conn.getRemoteSocketAddress());
            currentConnection = conn;
            conn.send("连接成功～");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            showToast("断开: " + conn.getRemoteSocketAddress());
            if (currentConnection == conn) {
                currentConnection = null;
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Log.i("--==>", "收到socket的消息 is：" + message);
            try {
                JSONObject json = new JSONObject(message);
                String type = json.getString("type");
                if (type.equals("offer")) {
                    String sdp = json.getString("sdp");
                    if (onOfferReceived != null) {
                        onOfferReceived.onReceived(sdp);
                    }
                } else if (type.equals("answer")) {
                    String sdp = json.getString("sdp");
                    if (onAnswerReceived != null) {
                        onAnswerReceived.onReceived(sdp);
                    }
                } else if (type.equals("candidate")) {
                    String sdpMid = json.getString("sdpMid");
                    int sdpMLineIndex = json.getInt("sdpMLineIndex");
                    String candidate = json.getString("candidate");
                    if (onIceCandidateReceived != null) {
                        onIceCandidateReceived.onReceived(sdpMid, sdpMLineIndex, candidate);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
        }

        @Override
        public void onStart() {
            showToast("服务启动成功");
        }
    }

    private void showToast(String message) {
        mHandler.post(() -> {
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        });
    }

    public void sendAnswer(SessionDescription sessionDescription) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "answer");
            json.put("sdp", sessionDescription.description);
            currentConnection.send(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendIceCandidate(IceCandidate iceCandidate) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "candidate");
            json.put("sdpMid", iceCandidate.sdpMid);
            json.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            json.put("candidate", iceCandidate.sdp);
            currentConnection.send(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface OnOfferReceivedListener {
        void onReceived(String sdp);
    }

    public interface OnAnswerReceivedListener {
        void onReceived(String sdp);
    }

    public interface OnIceCandidateReceivedListener {
        void onReceived(String sdpMid, int sdpMLineIndex, String candidate);
    }

    public void setOnOfferReceivedListener(OnOfferReceivedListener listener) {
        this.onOfferReceived = listener;
    }

    public void setOnAnswerReceivedListener(OnAnswerReceivedListener listener) {
        this.onAnswerReceived = listener;
    }

    public void setOnIceCandidateReceivedListener(OnIceCandidateReceivedListener listener) {
        this.onIceCandidateReceived = listener;
    }
}
