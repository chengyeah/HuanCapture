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
        private String type;
        private String sdp;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSdp() {
            return sdp;
        }

        public void setSdp(String sdp) {
            this.sdp = sdp;
        }

        @Override
        public String toString() {
            return "ActionBeanTDO{" +
                    "type='" + type + '\'' +
                    ", sdp='" + sdp + '\'' +
                    '}';
        }
    }
}
