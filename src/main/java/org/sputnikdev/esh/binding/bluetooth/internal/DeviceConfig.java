package org.sputnikdev.esh.binding.bluetooth.internal;

/**
 * Bluetooth device configuration.
 *
 * @author Vlad Kolotov
 */
public class DeviceConfig {

    /**
     * Predefined values for the Kalman filter that is used to smooth RSSI readings.
     */
    public enum RssiFilterType {
        //TODO revise this settings
        NONE(0, 0),
        FAST(0.125, 0.8),
        MODERATE(0.125, 5),
        SLOW(0.125, 15);

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
    private String preferredBluetoothAdapter;

    /**
     * Returns a timeout value which is used to determine if a bluetooth device gets offline (in seconds).
     * Bluetooth devices not showing any activity are considered to be offline after exceeding this timeout.
     * @return online timeout
     */
    public Integer getOnlineTimeout() {
        return onlineTimeout;
    }

    /**
     * Sets a timeout value which is used to determine if a bluetooth device gets offline (in seconds).
     * Bluetooth devices not showing any activity are considered to be offline after exceeding this timeout.
     * @param onlineTimeout online timeout
     */
    public void setOnlineTimeout(Integer onlineTimeout) {
        this.onlineTimeout = onlineTimeout;
    }

    /**
     * Returns RSSI filter type out of the predefined values {@link RssiFilterType}.
     * @return RSSI filter type
     */
    public String getRssiFilterType() {
        return rssiFilterType;
    }

    /**
     * Sets RSSI filter type out of the predefined values {@link RssiFilterType}.
     * @param rssiFilterType RSSI filter type
     */
    public void setRssiFilterType(String rssiFilterType) {
        this.rssiFilterType = rssiFilterType;
    }

    /**
     * Returns measured/estimated (user defined) TX power of the device that is measured 1 meter away from the adapter.
     * TX power is used in distance calculation.
     * @return measured TX power
     */
    public Integer getTxPowerMeasured() {
        return txPowerMeasured;
    }

    /**
     * Sets measured/estimated (user defined) TX power of the device that is measured 1 meter away from the adapter.
     * TX power is used in distance calculation.
     * @param txPowerMeasured measured TX power
     */
    public void setTxPowerMeasured(Integer txPowerMeasured) {
        this.txPowerMeasured = txPowerMeasured;
    }

    /**
     * Returns connection strategy that is used when device "connection control" is enabled.
     * The following strategies are supported:
     * <ul>
     * <li>Nearest adapter: the nearest adapter is selected automatically. Note: the selected adapter won't be
     * changed while the device is connected even if the selected adapter is no longer the nearest.
     * If disconnected, a new nearest adapter might be selected.</li>
     * <li>Preferred adapter: adapter is specified by user.</li>
     * </ul>
     * @return connection strategy
     */
    public String getConnectionStrategy() {
        return connectionStrategy;
    }

    /**
     * Sets connection strategy that is used when device "connection control" is enabled.
     * The following strategies are supported:
     * <ul>
     * <li>Nearest adapter: the nearest adapter is selected automatically. Note: the selected adapter won't be
     * changed while the device is connected even if the selected adapter is no longer the nearest.
     * If disconnected, a new nearest adapter might be selected.</li>
     * <li>Preferred adapter: adapter is specified by user.</li>
     * </ul>
     * @param connectionStrategy connection strategy
     */
    public void setConnectionStrategy(String connectionStrategy) {
        this.connectionStrategy = connectionStrategy;
    }

    /**
     * Returns a preferred adapter MAC address. Note: it is used only when "Preferred adapter" connection
     * strategy is set.
     * @return preferred adapter
     */
    public String getPreferredBluetoothAdapter() {
        return preferredBluetoothAdapter;
    }

    /**
     * Sets a preferred adapter MAC address. Note: it is used only when "Preferred adapter" connection
     * strategy is set.
     * @param preferredBluetoothAdapter preferred adapter
     */
    public void setPreferredBluetoothAdapter(String preferredBluetoothAdapter) {
        this.preferredBluetoothAdapter = preferredBluetoothAdapter;
    }
}
