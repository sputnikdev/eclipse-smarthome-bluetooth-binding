package org.sputnikdev.esh.binding.bluetooth.internal;

import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bluetooth binding configuration.
 *
 * @author Vlad Kolotov
 */
public class BluetoothBindingConfig {

    public enum GattParsingStrategy {
        /**
         * Human-readable channels are created for only recognised attributes,
         * all other "unknown" attributes are ignored.
         */
        RECOGNISED_ONLY,
        /**
         * Human-readable channels are created for recognised attributes, binary channels are created for
         * all other "unknown" attributes.
         */
        UNRECOGNISED_AS_BINARY,
        /**
         * All channels are binary.
         */
        ALL_BINARY
    }

    private String extensionFolder = BluetoothBindingConstants.DEFAULT_EXTENSION_FOLDER;
    private int updateRate = BluetoothBindingConstants.DEFAULT_UPDATE_RATE;
    private int initialOnlineTimeout = BluetoothBindingConstants.DEFAULT_ONLINE_TIMEOUT;
    private boolean initialConnectionControl = BluetoothBindingConstants.DEFAULT_CONNECTION_CONTROL;
    private String serialPortRegex;
    private boolean combinedDevicesEnabled = true;
    private Set<String> advancedGattServices = new HashSet<>();
    private String gattParsingStrategy = GattParsingStrategy.RECOGNISED_ONLY.name();
    private long rssiReportingRate = BluetoothBindingConstants.DEFAULT_RSS_REPORTING_RATE;

    public BluetoothBindingConfig() {
        advancedGattServices.addAll(Arrays.asList("00001800-0000-1000-8000-00805f9b34fb",
                "00001801-0000-1000-8000-00805f9b34fb"));
    }

    /**
     * Returns a path to a folder containing definitions for custom GATT services and characteristics
     * ({@link org.sputnikdev.bluetooth.gattparser.BluetoothGattParser#loadExtensionsFromFolder(String)}).
     * @return a path to a folder
     */
    public String getExtensionFolder() {
        return extensionFolder;
    }

    /**
     * Sets a path to a folder containing definitions for custom GATT services and characteristics
     * ({@link org.sputnikdev.bluetooth.gattparser.BluetoothGattParser#loadExtensionsFromFolder(String)}).
     * @param extensionFolder a path to a folder
     */
    public void setExtensionFolder(String extensionFolder) {
        this.extensionFolder = extensionFolder;
    }

    /**
     * Returns how often (in seconds) bluetooth adapters and devices are checked/updated.
     * @return bluetooth adapters and devices update rate
     */
    public int getUpdateRate() {
        return updateRate;
    }

    /**
     * Sets how often (in seconds) bluetooth adapters and devices should be checked/updated.
     * @param updateRate bluetooth adapters and devices update rate
     */
    public void setUpdateRate(int updateRate) {
        this.updateRate = updateRate;
    }

    /**
     * Returns an initial timeout value (can be defined for each bluetooth thing) which is used to determine
     * if a bluetooth device gets offline (in seconds). Bluetooth devices not showing any activity are considered
     * to be offline after exceeding this timeout.
     * @return initial online timeout
     */
    public int getInitialOnlineTimeout() {
        return initialOnlineTimeout;
    }

    /**
     * Sets an initial timeout value (can be defined for each bluetooth thing) which is used to determine
     * if a bluetooth device gets offline (in seconds). Bluetooth devices not showing any activity are considered
     * to be offline after exceeding this timeout.
     * @param initialOnlineTimeout initial online timeout
     */
    public void setInitialOnlineTimeout(int initialOnlineTimeout) {
        this.initialOnlineTimeout = initialOnlineTimeout;
    }

    /**
     * Returns initial connection control (default) for newly adding bluetooth devices. If set to true, connection
     * is kept on for newly added bluetooth devices, otherwise off. Can be customized for each bluetooth device.
     * @return initial connection control
     */
    public boolean isInitialConnectionControl() {
        return initialConnectionControl;
    }

    /**
     * Sets initial connection control (default) for newly adding bluetooth devices. If set to true, connection
     * is kept on for newly added bluetooth devices, otherwise off. Can be customized for each bluetooth device.
     * @param initialConnectionControl initial connection control
     */
    public void setInitialConnectionControl(boolean initialConnectionControl) {
        this.initialConnectionControl = initialConnectionControl;
    }

    /**
     * Returns a regular expression for BlueGiga adapters autodiscovery (serial ports).
     * @return a regular expression for BlueGiga adapters autodiscovery
     */
    public String getSerialPortRegex() {
        return serialPortRegex;
    }

    /**
     * Sets a regular expression for BlueGiga adapters autodiscovery (serial ports).
     * @param serialPortRegex a regular expression for BlueGiga adapters autodiscovery
     */
    public void setSerialPortRegex(String serialPortRegex) {
        this.serialPortRegex = serialPortRegex;
    }

    /**
     * Returns a list of GATT service UUIDs to be excluded from the automatic channel linkage.
     * No items/channels will be automatically created for these services.
     * @return list of GATT service UUIDs
     */
    public Set<String> getAdvancedGattServices() {
        return advancedGattServices;
    }

    /**
     * Sets a list of GATT service UUIDs to be excluded from the automatic channel linkage.
     * No items/channels will be automatically created for these services.
     * @param advancedGattServices list of GATT service UUIDs
     */
    public void setAdvancedGattServices(List<String> advancedGattServices) {
        this.advancedGattServices = Collections.unmodifiableSet(
                advancedGattServices.stream().map(String::trim).map(String::toLowerCase).collect(Collectors.toSet()));
    }

    /**
     * Returns GATT services and characteristics parsing strategy. Controls how recognised and unrecognised
     * GATT attributes are handled.
     * @return GATT services and characteristics parsing strategy
     */
    public String getGattParsingStrategy() {
        return gattParsingStrategy;
    }

    /**
     * Returns GATT services and characteristics parsing strategy formatted as GattParsingStrategy enum.
     * @return GATT services and characteristics parsing strategy
     */
    public GattParsingStrategy getGattStrategy() {
        return GattParsingStrategy.valueOf(gattParsingStrategy);
    }

    public boolean discoverUnknown() {
        return GattParsingStrategy.valueOf(gattParsingStrategy) != GattParsingStrategy.RECOGNISED_ONLY;
    }

    public boolean discoverRecognisedOnly() {
        return GattParsingStrategy.valueOf(gattParsingStrategy) == GattParsingStrategy.RECOGNISED_ONLY;
    }

    public boolean discoverBinaryOnly() {
        return GattParsingStrategy.valueOf(gattParsingStrategy) == GattParsingStrategy.ALL_BINARY;
    }

    /**
     * Sets GATT services and characteristics parsing strategy. Controls how recognised and unrecognised
     * GATT attributes are handled.
     * @param gattParsingStrategy GATT services and characteristics parsing strategy
     */
    public void setGattParsingStrategy(String gattParsingStrategy) {
        this.gattParsingStrategy = gattParsingStrategy;
    }

    /**
     * Experimental feature (for debugging). If set to false, bluetooth device things are duplicated for each
     * installed bluetooth adapter.
     *
     * @return false if bluetooth device things are duplicated for each adapter
     */
    public boolean isCombinedDevicesEnabled() {
        return combinedDevicesEnabled;
    }

    /**
     * Experimental feature (for debugging). If set to false, bluetooth device things are duplicated for each
     * installed bluetooth adapter.
     *
     * @param combinedDevicesEnabled if set to false, bluetooth device things are duplicated for each adapter
     */
    public void setCombinedDevicesEnabled(boolean combinedDevicesEnabled) {
        this.combinedDevicesEnabled = combinedDevicesEnabled;
    }

    /**
     * Returns how frequently RSSI is reported (in ms). This parameter does not affect
     * the "indoor positioning system". Important note: setting it to a low value might cause an excessive CPU load.
     * @return reporting rate in milliseconds
     */
    public long getRssiReportingRate() {
        return rssiReportingRate;
    }

    /**
     * Controls how frequently RSSI should be reported (in ms). This parameter does not affect
     * the "indoor positioning system". Important note: setting it to a low value might cause an excessive CPU load.
     * @param rssiReportingRate reporting rate in milliseconds
     */
    public void setRssiReportingRate(long rssiReportingRate) {
        this.rssiReportingRate = rssiReportingRate;
    }
}
