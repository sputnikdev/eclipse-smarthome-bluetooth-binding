package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.StringType;

/**
 * @author Vlad Kolotov
 */
class StringTypeChannelHandler extends SingleChannelHandler<String, StringType> {

    StringTypeChannelHandler(BluetoothHandler handler, String channelID, boolean persistent) {
        super(handler, channelID, persistent);
    }

    StringTypeChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override
    String convert(StringType value) {
        return value != null ? value.toString() : null;
    }

    @Override
    StringType convert(String value) {
        return value != null ? new StringType(value) : null;
    }

    @Override
    String load(Object stored) {
        return stored instanceof String ? (String) stored : null;
    }
}
