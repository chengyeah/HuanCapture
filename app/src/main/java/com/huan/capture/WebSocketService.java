package com.huan.capture;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketService {

    private static final String TAG = "WebSocketService";
    private WebSocketClient webSocketClient;

    public WebSocketService(String serverUri) {
        URI uri;
        try {
            uri = new URI(serverUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "WebSocket opened");
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "WebSocket message: " + message);
                try {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");
                    if (type.equals("offer")) {
                        String sdp = json.getString("sdp");
                        // 处理 offer
                    } else if (type.equals("answer")) {
                        String sdp = json.getString("sdp");
                        // 处理 answer
                    } else if (type.equals("candidate")) {
                        String sdpMid = json.getString("sdpMid");
                        int sdpMLineIndex = json.getInt("sdpMLineIndex");
                        String candidate = json.getString("candidate");
                        // 处理 candidate
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "WebSocket closed");
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "WebSocket error", ex);
            }
        };

        webSocketClient.connect();
    }

    public void sendOffer(SessionDescription sessionDescription) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "offer");
            json.put("sdp", sessionDescription.description);
            webSocketClient.send(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}

