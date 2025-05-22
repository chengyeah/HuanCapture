package org.webrtc.ext;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.EglBase;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFactory;

import java.util.ArrayList;
import java.util.List;

public class H264OnlyDecoderFactory implements VideoDecoderFactory {
    private final DefaultVideoDecoderFactory baseFactory;

    public H264OnlyDecoderFactory(EglBase.Context eglContext) {
        this.baseFactory = new DefaultVideoDecoderFactory(eglContext);
    }

    @Override
    public VideoDecoder createDecoder(VideoCodecInfo info) {
        if ("H264".equalsIgnoreCase(info.name)) {
            return baseFactory.createDecoder(info);
        }
        return null;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> supported = new ArrayList<>();
        for (VideoCodecInfo info : baseFactory.getSupportedCodecs()) {
            if ("H264".equalsIgnoreCase(info.name)) {
                supported.add(info);
            }
        }
        return supported.toArray(new VideoCodecInfo[0]);
    }
}