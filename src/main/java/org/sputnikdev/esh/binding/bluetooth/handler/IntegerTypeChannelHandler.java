package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;

import java.math.BigDecimal;

/**
 * Integer type channel handler.
 *
 * @author Vlad Kolotov
 */
abstract class IntegerTypeChannelHandler extends SingleChannelHandler<Integer, DecimalType> {

    IntegerTypeChannelHandler(BluetoothHandler handler, String channelID, boolean persistent) {
        super(handler, channelID, persistent);
    }

    IntegerTypeChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override protected Integer convert(DecimalType value) {
        return value != null ? value.intValue() : null;
    }

    @Override protected DecimalType convert(Integer value) {
        return value != null ? new DecimalType(value) : null;
    }

    @Override protected Integer load(Object stored) {
        return stored instanceof BigDecimal ? ((BigDecimal) stored).intValue() : null;
    }
}
