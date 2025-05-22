package org.webrtc.ext;

import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.List;

public class H264OnlyEncoderFactory implements VideoEncoderFactory {
    private final VideoEncoderFactory baseFactory;

    public H264OnlyEncoderFactory(EglBase.Context eglContext) {
        this.baseFactory = new DefaultVideoEncoderFactory(eglContext, true, true);
    }

    @Override
    public VideoEncoder createEncoder(VideoCodecInfo info) {
        if (info.name.equalsIgnoreCase("H264")) {
            VideoEncoder encoder = baseFactory.createEncoder(info);
            return new H264Encoder(encoder);
//            return baseFactory.createEncoder(info);
        }
        return null;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> filtered = new ArrayList<>();
        for (VideoCodecInfo info : baseFactory.getSupportedCodecs()) {
            if (info.name.equalsIgnoreCase("H264")) {
                filtered.add(info);
            }
        }
        return filtered.toArray(new VideoCodecInfo[0]);
    }
}