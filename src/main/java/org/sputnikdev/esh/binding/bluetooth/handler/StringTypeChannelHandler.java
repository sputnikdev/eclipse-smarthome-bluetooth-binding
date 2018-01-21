package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.StringType;

/**
 * String type channel handler.
 *
 * @author Vlad Kolotov
 */
abstract class StringTypeChannelHandler extends SingleChannelHandler<String, StringType> {

    StringTypeChannelHandler(BluetoothHandler handler, String channelID, boolean persistent) {
        super(handler, channelID, persistent);
    }

    StringTypeChannelHandler(BluetoothHandler handler, String channelID) {
        super(handler, channelID);
    }

    @Override protected String convert(StringType value) {
        return value != null ? value.toString() : null;
    }

    @Override protected StringType convert(String value) {
        return value != null ? new StringType(value) : null;
    }

    @Override protected String load(Object stored) {
        return stored instanceof String ? (String) stored : null;
    }
}
