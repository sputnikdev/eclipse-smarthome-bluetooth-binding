package org.sputnikdev.esh.binding.bluetooth.internal;

import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Bluetooth binding configuration.
 *
 * @author Vlad Kolotov
 */
public class BluetoothBindingConfig {

    private String extensionFolder = BluetoothBindingConstants.DEFAULT_EXTENSION_FOLDER;
    private int updateRate = BluetoothBindingConstants.DEFAULT_UPDATE_RATE;
    private int initialOnlineTimeout = BluetoothBindingConstants.DEFAULT_ONLINE_TIMEOUT;
    private boolean initialConnectionControl = BluetoothBindingConstants.DEFAULT_CONNECTION_CONTROL;
    private String serialPortRegex;
    private boolean combinedDevicesEnabled = true;
    private List<String> advancedGattServices = new ArrayList<>();
    private boolean discoverUnknownAttributes;

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
     * @return a regular expression for BlueGiga adapters autodiscovery
     */
    public void setSerialPortRegex(String serialPortRegex) {
        this.serialPortRegex = serialPortRegex;
    }

    public List<String> getAdvancedGattServices() {
        return advancedGattServices;
    }

    public void setAdvancedGattServices(List<String> advancedGattServices) {
        this.advancedGattServices = Collections.unmodifiableList(advancedGattServices);
    }

    public boolean isDiscoverUnknownAttributes() {
        return discoverUnknownAttributes;
    }

    public void setDiscoverUnknownAttributes(boolean discoverUnknownAttributes) {
        this.discoverUnknownAttributes = discoverUnknownAttributes;
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
     * @param if set to false, bluetooth device things are duplicated for each adapter
     */
    public void setCombinedDevicesEnabled(boolean combinedDevicesEnabled) {
        this.combinedDevicesEnabled = combinedDevicesEnabled;
    }
}
