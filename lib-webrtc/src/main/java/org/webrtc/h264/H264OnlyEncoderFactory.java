package org.webrtc.h264;

import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFrame;

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
            VideoEncoder baseEncoder = baseFactory.createEncoder(info);
            return new VideoEncoder() {
                @Override
                public VideoCodecStatus initEncode(Settings origin, Callback encodeCallback) {
                    Settings settings = new Settings(
                            origin.numberOfCores,
                            origin.width,
                            origin.height,
                            origin.startBitrate,
                            20,
                            origin.numberOfSimulcastStreams,
                            origin.automaticResizeOn,
                            origin.capabilities
                    );
                    return baseEncoder.initEncode(settings, encodeCallback);
                }

                @Override
                public VideoCodecStatus release() {
                    return baseEncoder.release();
                }

                @Override
                public VideoCodecStatus encode(VideoFrame frame, EncodeInfo info) {
                    return baseEncoder.encode(frame, info);
                }

                @Override
                public VideoCodecStatus setRateAllocation(BitrateAllocation allocation, int framerate) {
                    return baseEncoder.setRateAllocation(allocation, framerate);
                }

                @Override
                public ScalingSettings getScalingSettings() {
                    return baseEncoder.getScalingSettings();
                }

                @Override
                public String getImplementationName() {
                    return baseEncoder.getImplementationName();
                }
            };
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