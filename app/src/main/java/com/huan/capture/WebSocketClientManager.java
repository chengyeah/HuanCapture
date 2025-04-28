package com.huan.capture;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketClientManager {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private WebSocketClient mWebSocketClient;
    private final Context mContext;

    public WebSocketClientManager(Context context) {
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
                try {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");
                    if (type.equals("answer")) {
                        String sdp = json.getString("sdp");
                        if (onAnswerReceived != null) {
                            onAnswerReceived.onReceived(new SessionDescription(SessionDescription.Type.ANSWER, sdp));
                        }
                    } else if (type.equals("candidate")) {
                        String sdpMid = json.getString("sdpMid");
                        int sdpMLineIndex = json.getInt("sdpMLineIndex");
                        String candidate = json.getString("candidate");
                        if (onIceCandidateReceived != null) {
                            onIceCandidateReceived.onReceived(new IceCandidate(sdpMid, sdpMLineIndex, candidate));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

    public void sendAnswer(SessionDescription sessionDescription) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "answer");
            json.put("sdp", sessionDescription.description);
            sendToServer(json.toString());
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
            sendToServer(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendOffer(SessionDescription sessionDescription) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "offer");
            json.put("sdp", sessionDescription.description);
            sendToServer(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface OnAnswerReceivedListener {
        void onReceived(SessionDescription sessionDescription);
    }

    public interface OnIceCandidateReceivedListener {
        void onReceived(IceCandidate iceCandidate);
    }

    private OnAnswerReceivedListener onAnswerReceived;
    private OnIceCandidateReceivedListener onIceCandidateReceived;

    public void setOnAnswerReceivedListener(OnAnswerReceivedListener listener) {
        this.onAnswerReceived = listener;
    }

    public void setOnIceCandidateReceivedListener(OnIceCandidateReceivedListener listener) {
        this.onIceCandidateReceived = listener;
    }
}
