package org.sputnikdev.esh.binding.bluetooth.internal;

import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.manager.BluetoothManager;

/**
 * A helper structure which contains all necessary objects for the bluetooth thing handlers functioning.
 *
 * @author Vlad Kolotov
 */
public class BluetoothContext {

    private final BluetoothManager manager;
    private final BluetoothGattParser parser;
    private final BluetoothBindingConfig config;

    BluetoothContext(BluetoothManager manager, BluetoothGattParser parser,
                            BluetoothBindingConfig config) {
        this.manager = manager;
        this.parser = parser;
        this.config = config;
    }

    /**
     * Returns bluetooth manager.
     * @return bluetooth manager
     */
    public BluetoothManager getManager() {
        return manager;
    }

    /**
     * Returns GATT parser.
     * @return GATT parser
     */
    public BluetoothGattParser getParser() {
        return parser;
    }

    /**
     * Returns bluetooth binding config.
     * @return bluetooth binding config
     */
    public BluetoothBindingConfig getConfig() {
        return config;
    }
}
