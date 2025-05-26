package org.webrtc.factory;

import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.List;

public class H264OnlyVideoEncoderFactory implements VideoEncoderFactory {
    private final HardwareVideoEncoderFactory internalFactory;

    public H264OnlyVideoEncoderFactory(EglBase.Context eglContext) {
        internalFactory = new HardwareVideoEncoderFactory(eglContext, false, true);
    }

    @Override
    public VideoEncoder createEncoder(VideoCodecInfo info) {
        if ("H264".equalsIgnoreCase(info.name)) {
            return internalFactory.createEncoder(info);
        }
        return null;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> supportedCodecs = new ArrayList<>();
        for (VideoCodecInfo info : internalFactory.getSupportedCodecs()) {
            if ("H264".equalsIgnoreCase(info.name)) {
                supportedCodecs.add(info);
            }
        }
        return supportedCodecs.toArray(new VideoCodecInfo[0]);
    }
}
