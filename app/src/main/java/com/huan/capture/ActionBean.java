package com.huan.capture;

public class ActionBean {
    private String action;
    private String args;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return "ActionBean{" +
                "action='" + action + '\'' +
                ", args='" + args + '\'' +
                '}';
    }

    public static class ActionBeanTDO {
        private String action;
        private String sdp;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getSdp() {
            return sdp;
        }

        public void setSdp(String sdp) {
            this.sdp = sdp;
        }
    }
}
