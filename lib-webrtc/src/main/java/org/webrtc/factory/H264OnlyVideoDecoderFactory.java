package org.webrtc.factory;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.EglBase;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoDecoder;
import org.webrtc.VideoDecoderFactory;

import java.util.ArrayList;
import java.util.List;

public class H264OnlyVideoDecoderFactory implements VideoDecoderFactory {
    private final DefaultVideoDecoderFactory internalFactory;

    public H264OnlyVideoDecoderFactory(EglBase.Context eglContext) {
        internalFactory = new DefaultVideoDecoderFactory(eglContext);
    }

    @Override
    public VideoDecoder createDecoder(VideoCodecInfo info) {
        if ("H264".equalsIgnoreCase(info.name)) {
            return internalFactory.createDecoder(info);
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
