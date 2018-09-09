package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;

/**
 * A bluetooth handler which represents bluetooth beacons.
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class BeaconBluetoothDeviceHandler extends GenericBluetoothDeviceHandler {

    private Logger logger = LoggerFactory.getLogger(BeaconBluetoothDeviceHandler.class);

    public BeaconBluetoothDeviceHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing, bluetoothContext);
    }

}
