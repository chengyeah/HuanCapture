package com.huan.capture;

import static org.webrtc.SessionDescription.Type.ANSWER;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import eskit.sdk.support.messenger.client.EsMessenger;
import eskit.sdk.support.messenger.client.bean.EsEvent;
import eskit.sdk.support.messenger.client.core.EsCommand;

/**
 * <br>
 *
 * <br>
 */
public class BaseTransferActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConfigParams.getInstance().setOnClientMessageListener(new ConfigParams.OnClientMessageListener() {
            @Override
            public void onMessage(EsEvent esEvent) {
                Log.i("--==>", "onMessage: " + esEvent.toString());
                Gson gson = new Gson();
                ActionBean actionBean = gson.fromJson(esEvent.getData(), ActionBean.class);
                switch (actionBean.getAction()) {
                    case "answer":
                        Log.i("--==>", "onMessage------>: " + actionBean.getArgs());
                        ActionBean.ActionBeanTDO actionBeanTDO = gson.fromJson(actionBean.getArgs(), ActionBean.ActionBeanTDO.class);
                        Log.i("--==>", "onMessage------====>: " + actionBeanTDO.getSdp());
                        onReceiveAnswer(new SessionDescription(ANSWER, actionBeanTDO.getSdp()));
                        break;
                }
            }
        });
    }

    protected void onReceiveAnswer(SessionDescription sessionDescription) {
    }

    protected void sendIceCandidateToTV(IceCandidate iceCandidate) {
        EsCommand.CmdArgs args = new EsCommand.CmdArgs("home")
                .put("action", "candidate")
                .put("sdpMid", iceCandidate.sdpMid)
                .put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                .put("candidate", iceCandidate.sdp);
        EsCommand cmd = EsCommand.makeCustomCommand("OnLinkWebRTC")
                .setEventData(args);

        EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
    }

    protected void sendOfferToTV(SessionDescription sessionDescription) {
        String sdpFormat = sessionDescription.description.replaceAll("\r\n", "#");
        Log.i("--==>", "处理后offer数据 : " + sdpFormat);
        // 分片发送
        int maxLength = 500;
        int totalChunks = (int) Math.ceil((double) sdpFormat.length() / maxLength);

        for (int i = 0; i < sdpFormat.length(); i += maxLength) {
            int end = Math.min(i + maxLength, sdpFormat.length());
            String chunk = sdpFormat.substring(i, end);
            int chunkNumber = i / maxLength + 1;

            EsCommand.CmdArgs args = new EsCommand.CmdArgs("home")
                    .put("action", "offer")
                    .put("sdp", chunk)
                    .put("chunkNumber", chunkNumber)
                    .put("totalChunks", totalChunks);
            EsCommand cmd = EsCommand.makeCustomCommand("OnLinkWebRTC")
                    .setEventData(args);

            EsMessenger.get().sendCommand(this, ConfigParams.mEsDevice, cmd);
        }
    }

}
