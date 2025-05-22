package com.huan.capture;

import org.webrtc.PeerConnection;
import org.webrtc.StatsReport;

import java.util.Timer;
import java.util.TimerTask;

public class WebRTCStatsMonitor {

    private final PeerConnection peerConnection;
    private final long intervalMs;
    private final Timer statsTimer;

    public WebRTCStatsMonitor(PeerConnection peerConnection, long intervalMs) {
        this.peerConnection = peerConnection;
        this.intervalMs = intervalMs;
        this.statsTimer = new Timer(true);
    }

    public void start() {
        statsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                peerConnection.getStats(reports -> handleStats(reports), null);
            }
        }, 0, intervalMs);
    }

    public void stop() {
        statsTimer.cancel();
    }

    private void handleStats(StatsReport[] reports) {
        for (StatsReport report : reports) {
            switch (report.type) {
                case "inbound-rtp":
                case "outbound-rtp":
                    printRtpStats(report);
                    break;
                case "candidate-pair":
                    printCandidatePairStats(report);
                    break;
            }
        }
    }

    private void printRtpStats(StatsReport report) {
        String id = report.id;
        String kind = report.type;
        Long bytes = null;
        Long packets = null;
        Long packetsLost = null;
        Double jitter = null;

        for (StatsReport.Value value : report.values) {
            switch (value.name) {
                case "bytesReceived":
                case "bytesSent":
                    bytes = parseLong(value.value);
                    break;
                case "packetsReceived":
                case "packetsSent":
                    packets = parseLong(value.value);
                    break;
                case "packetsLost":
                    packetsLost = parseLong(value.value);
                    break;
                case "jitter":
                    jitter = parseDouble(value.value);
                    break;
            }
        }

        System.out.println("[" + kind + "] id=" + id +
                " bytes=" + bytes +
                " packets=" + packets +
                " lost=" + packetsLost +
                " jitter=" + jitter);
    }

    private void printCandidatePairStats(StatsReport report) {
        String state = null;
        String nominated = null;
        String currentRoundTripTime = null;
        String availableOutgoingBitrate = null;
        String availableIncomingBitrate = null;

        for (StatsReport.Value value : report.values) {
            switch (value.name) {
                case "state":
                    state = value.value;
                    break;
                case "nominated":
                    nominated = value.value;
                    break;
                case "currentRoundTripTime":
                    currentRoundTripTime = value.value;
                    break;
                case "availableOutgoingBitrate":
                    availableOutgoingBitrate = value.value;
                    break;
                case "availableIncomingBitrate":
                    availableIncomingBitrate = value.value;
                    break;
            }
        }

        if ("succeeded".equals(state) && "true".equals(nominated)) {
            System.out.println("[candidate-pair] RTT=" + currentRoundTripTime +
                    " up=" + availableOutgoingBitrate +
                    " down=" + availableIncomingBitrate);
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }
}