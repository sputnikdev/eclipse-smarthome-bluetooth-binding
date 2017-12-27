/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.sputnikdev.esh.binding.bluetooth;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link BluetoothBindingConstants} class defines common constants, which are
 * used across the whole binding.
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class BluetoothBindingConstants {

    public static final String BINDING_ID = "bluetooth";
    
    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ADAPTER = new ThingTypeUID(BINDING_ID, "adapter");
    public static final ThingTypeUID THING_TYPE_GENERIC = new ThingTypeUID(BINDING_ID, "generic");
    public static final ThingTypeUID THING_TYPE_BLE = new ThingTypeUID(BINDING_ID, "ble");

    // List of all Channel ids
    public static final String CHANNEL_CHARACTERISTIC = "characteristic";
    public static final String CHANNEL_CHARACTERISTIC_FIELD = "characteristic-field";
    public static final String CHANNEL_RSSI = "rssi";
    public static final String CHANNEL_TX_POWER = "tx-power";
    public static final String CHANNEL_ESTIMATED_DISTANCE = "estimated-distance";
    public static final String CHANNEL_LAST_UPDATED = "last-updated";
    public static final String CHANNEL_CONNECTED = "connected";
    public static final String CHANNEL_CONNECTION_CONTROL = "connection-control";
    public static final String CHANNEL_ONLINE = "online";
    public static final String CHANNEL_DISCOVERING = "discovering";
    public static final String CHANNEL_DISCOVERING_CONTROL = "discovering-control";
    public static final String CHANNEL_LOCATION = "location";
    public static final String CHANNEL_NEAREST_ADAPTER = "nearest-adapter";

    // Thing (device) properties
    public static final String PROPERTY_ADDRESS = "Address";
    public static final String PROPERTY_ADAPTER = "Adapter";

    // Field properties
    public static final String PROPERTY_FIELD_NAME = "FieldName";
    public static final String PROPERTY_FIELD_INDEX = "FieldIndex";

    // Characteristic properties
    public static final String PROPERTY_FLAGS = "Flags";
    public static final String PROPERTY_SERVICE_UUID = "ServiceUUID";
    public static final String PROPERTY_CHARACTERISTIC_UUID = "CharacteristicUUID";

    // Characteristic access flags
    public static final String READ_FLAG = "read";
    public static final String NOTIFY_FLAG = "notify";
    public static final String INDICATE_FLAG = "indicate";
    public static final String WRITE_FLAG = "write";

    // Configuration properties
    public static final int DEFAULT_UPDATE_RATE = 10;
    public static final int DEFAULT_ONLINE_TIMEOUT = 30;
    public static final boolean DEFAULT_CONNECTION_CONTROL = true;
    public static final String DEFAULT_EXTENSION_FOLDER = "/home/pi/.bluetooth_smart";
    public static final String DEFAULT_ADAPTERS_LOCATION = "Bluetooth Adapters";
    public static final String DEFAULT_DEVICES_LOCATION = "Bluetooth Devices";

}
