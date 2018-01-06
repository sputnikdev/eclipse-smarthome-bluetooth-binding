package org.sputnikdev.esh.binding.bluetooth.internal;

public class DeviceConfig {

    public enum RssiFilterType {
        //TODO rewise this settings
        NONE(0, 0),
        FAST(0.125, 0.8),
        MODERATE(0.125, 15),
        SLOW(0.125, 30);

        private final double processNoise;
        private final double measurmentNoise;

        RssiFilterType(double processNoise, double measurmentNoise) {
            this.processNoise = processNoise;
            this.measurmentNoise = measurmentNoise;
        }

        public double getProcessNoise() {
            return processNoise;
        }

        public double getMeasurmentNoise() {
            return measurmentNoise;
        }
    }

    private Integer onlineTimeout;
    private String rssiFilterType;
    private Integer txPowerMeasured;
    private String connectionStrategy;
    private String preferredAdapter;

    public Integer getOnlineTimeout() {
        return onlineTimeout;
    }

    public void setOnlineTimeout(Integer onlineTimeout) {
        this.onlineTimeout = onlineTimeout;
    }

    public String getRssiFilterType() {
        return rssiFilterType;
    }

    public void setRssiFilterType(String rssiFilterType) {
        this.rssiFilterType = rssiFilterType;
    }

    public Integer getTxPowerMeasured() {
        return txPowerMeasured;
    }

    public void setTxPowerMeasured(Integer txPowerMeasured) {
        this.txPowerMeasured = txPowerMeasured;
    }

    public String getConnectionStrategy() {
        return connectionStrategy;
    }

    public void setConnectionStrategy(String connectionStrategy) {
        this.connectionStrategy = connectionStrategy;
    }

    public String getPreferredAdapter() {
        return preferredAdapter;
    }

    public void setPreferredAdapter(String preferredAdapter) {
        this.preferredAdapter = preferredAdapter;
    }
}
