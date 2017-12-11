package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;

/**
 * @author Vlad Kolotov
 */
class DoubleTypeChannelHandler extends SingleChannelHandler<Double, DecimalType> {

    DoubleTypeChannelHandler(BluetoothHandler handler, String channelID, boolean persistent) {
        super(handler, channelID, persistent);
    }

    DoubleTypeChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override Double convert(DecimalType value) {
        return value != null ? value.doubleValue() : null;
    }

    @Override DecimalType convert(Double value) {
        return new DecimalType(value);
    }
}
