package com.huan.capture;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class WebSocketClientActivity extends AppCompatActivity {

    private XYWebSocketClientManager webSocket;
    private JSONObject jsonObject = new JSONObject();
    private final float MOVE_THRESHOLD = 1;
    private float lastX = 0;
    private float lastY = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_socket_client);

        View actionView = findViewById(R.id.actionView);
        EditText etIp = findViewById(R.id.etIp);
        etIp.setText("192.168.7.233");
        EditText etPort = findViewById(R.id.etPort);
        etPort.setText("38838");

        webSocket = new XYWebSocketClientManager(this);
        webSocket.connectToServer("ws://" + etIp.getText().toString().trim() + ":" + etPort.getText().toString().trim());

        // 假设 actionView 是你的 View 对象
        actionView.setOnTouchListener((v, event) -> {
            float x = event.getX() * 0.2f;
            float y = event.getY() * 0.2f;
            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 手指按下时触发
                        Log.d("Touch", "ACTION_DOWN at (" + x + ", " + y + ")");

                        jsonObject = new JSONObject();
                        jsonObject.put("type", "es_pointer_control");
                        jsonObject.put("action", 0);
                        jsonObject.put("keycode", 0);
                        jsonObject.put("x", x);
                        jsonObject.put("y", y);
                        webSocket.sendToServer(jsonObject.toString());

                        lastX = x;
                        lastY = y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 手指移动时触发
                        if (Math.abs(x - lastX) > MOVE_THRESHOLD || Math.abs(y - lastY) > MOVE_THRESHOLD) {
                            Log.d("Touch", "ACTION_MOVE at (" + x + ", " + y + ")");

                            jsonObject = new JSONObject();
                            jsonObject.put("type", "es_pointer_control");
                            jsonObject.put("action", 2);
                            jsonObject.put("keycode", 0);
                            jsonObject.put("x", x);
                            jsonObject.put("y", y);
                            webSocket.sendToServer(jsonObject.toString());

                            lastX = x;
                            lastY = y;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // 手指抬起时触发
                        Log.d("Touch", "ACTION_UP at (" + x + ", " + y + ")");

                        jsonObject = new JSONObject();
                        jsonObject.put("type", "es_pointer_control");
                        jsonObject.put("action", 1);
                        jsonObject.put("keycode", 0);
                        jsonObject.put("x", x);
                        jsonObject.put("y", y);
                        webSocket.sendToServer(jsonObject.toString());
                        break;
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            // 返回 true 表示事件已被消费
            return true;
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.disconnect();
    }
}
