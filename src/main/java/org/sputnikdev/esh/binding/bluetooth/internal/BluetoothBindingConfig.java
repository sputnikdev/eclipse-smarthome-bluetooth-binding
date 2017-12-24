package org.sputnikdev.esh.binding.bluetooth.internal;

import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

public class BluetoothBindingConfig {

    private String extensionFolder = BluetoothBindingConstants.DEFAULT_EXTENSION_FOLDER;
    private int updateRate = BluetoothBindingConstants.DEFAULT_UPDATE_RATE;
    private int initialOnlineTimeout = BluetoothBindingConstants.DEFAULT_ONLINE_TIMEOUT;
    private boolean initialConnectionControl = BluetoothBindingConstants.DEFAULT_CONNECTION_CONTROL;
    private String serialPortRegex;
    private boolean advancedMode;

    public String getExtensionFolder() {
        return extensionFolder;
    }

    public void setExtensionFolder(String extensionFolder) {
        this.extensionFolder = extensionFolder;
    }

    public int getUpdateRate() {
        return updateRate;
    }

    public void setUpdateRate(int updateRate) {
        this.updateRate = updateRate;
    }

    public int getInitialOnlineTimeout() {
        return initialOnlineTimeout;
    }

    public void setInitialOnlineTimeout(int initialOnlineTimeout) {
        this.initialOnlineTimeout = initialOnlineTimeout;
    }

    public boolean isInitialConnectionControl() {
        return initialConnectionControl;
    }

    public void setInitialConnectionControl(boolean initialConnectionControl) {
        this.initialConnectionControl = initialConnectionControl;
    }

    public String getSerialPortRegex() {
        return serialPortRegex;
    }

    public void setSerialPortRegex(String serialPortRegex) {
        this.serialPortRegex = serialPortRegex;
    }

    public boolean isAdvancedMode() {
        return advancedMode;
    }

    public void setAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
    }
}
