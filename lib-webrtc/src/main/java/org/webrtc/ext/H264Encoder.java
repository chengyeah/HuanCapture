package org.webrtc.ext;

import org.webrtc.VideoCodecStatus;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoFrame;

/**
 * <br>
 *
 * <br>
 */
public class H264Encoder implements VideoEncoder {

    private final VideoEncoder baseEncoder;

    public H264Encoder(VideoEncoder baseEncoder) {
        this.baseEncoder = baseEncoder;
    }

    @Override
    public VideoCodecStatus initEncode(Settings settings, Callback encodeCallback) {
        // 强制修改配置，例如帧率、码率等
        settings = new Settings(
                settings.numberOfCores,
                settings.width,
                settings.height,
                2000, // 2Mbps 固定码率
                15,        // 固定帧率
                1,         // 1路码流
                false      // 不自动分辨率调整
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
}
