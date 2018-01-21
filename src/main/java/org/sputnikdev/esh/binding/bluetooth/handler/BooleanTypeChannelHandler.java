package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;

/**
 * Boolean type channel handler.
 *
 * @author Vlad Kolotov
 */
abstract class BooleanTypeChannelHandler extends SingleChannelHandler<Boolean, OnOffType> {

    BooleanTypeChannelHandler(BluetoothHandler handler, String channelID, boolean persistent) {
        super(handler, channelID, persistent);
    }

    BooleanTypeChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override protected Boolean convert(OnOffType value) {
        return value != null && value == OnOffType.ON;
    }

    @Override protected OnOffType convert(Boolean value) {
        return value != null && value ? OnOffType.ON : OnOffType.OFF;
    }

    @Override protected Boolean load(Object stored) {
        return stored instanceof Boolean ? (Boolean) stored : null;
    }
}
