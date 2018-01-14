package org.sputnikdev.esh.binding.bluetooth.internal;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.CombinedGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import java.util.Optional;
import java.util.Set;

/**
 * @author Vlad Kolotov
 */
public class BluetoothUtils {

    private static final String MAC_PART_REGEXP = "(\\w{2}(?=(\\w{2})))";

    private BluetoothUtils() {}

    public static ThingUID getAdapterUID(URL url) {
        return new ThingUID(BluetoothBindingConstants.THING_TYPE_ADAPTER, getUID(url.getAdapterAddress()));
    }

    public static ThingUID getDeviceUID(DiscoveredDevice device) {
        return getDeviceUID(device.getURL(), device.isBleEnabled());
    }

    public static ThingUID getDeviceUID(URL url, boolean bleEnabled) {
        ThingTypeUID thingTypeUID;
        if (isCombined(url)) {
            thingTypeUID = bleEnabled
                    ? BluetoothBindingConstants.THING_TYPE_BLE :
                    BluetoothBindingConstants.THING_TYPE_GENERIC;
            return new ThingUID(thingTypeUID, getUID(url.getDeviceAddress()));
        } else {
            thingTypeUID = bleEnabled
                    ? BluetoothBindingConstants.THING_TYPE_BLE_DEDICATED :
                    BluetoothBindingConstants.THING_TYPE_GENERIC_DEDICATED;
            return new ThingUID(thingTypeUID, getAdapterUID(url), getUID(url.getDeviceAddress()));
        }
    }

    public static URL getURL(Thing thing) {
        if (BluetoothBindingConstants.THING_TYPE_ADAPTER.equals(thing.getThingTypeUID())) {
            return new URL(getAddressFromUID(thing.getUID().getId()), null);
        } else {
            String adapterAddress = Optional.of(thing).map(Thing::getBridgeUID).map(ThingUID::getId)
                    .map(BluetoothUtils::getAddressFromUID).orElse(CombinedGovernor.COMBINED_ADDRESS);
            return new URL(adapterAddress, getAddressFromUID(thing.getUID().getId()));
        }
    }

    public static String getChannelUID(URL url) {
        String channelUID = getShortUUID(url.getServiceUUID()) + "-" + getShortUUID(url.getCharacteristicUUID());

        if (url.getFieldName() != null) {
            channelUID += "-" + url.getFieldName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        }

        return channelUID;
    }

    public static String getItemUID(URL url) {
        String itemUID;
        if (isCombined(url)) {
            itemUID = BluetoothBindingConstants.THING_TYPE_BLE.getAsString().replace(":", "_");
        } else {
            itemUID = BluetoothBindingConstants.THING_TYPE_BLE_DEDICATED.getAsString().replace(":", "_")
                    + "_" + getUID(url.getAdapterAddress());
        }
        itemUID += "_" + getUID(url.getDeviceAddress()) + "_" + getChannelUID(url).replaceAll("-", "_");
        return itemUID;
    }

    public static boolean hasNotificationAccess(Set<CharacteristicAccessType> flags) {
        return flags.contains(CharacteristicAccessType.NOTIFY) ||
                flags.contains(CharacteristicAccessType.INDICATE);
    }

    public static boolean hasReadAccess(Set<CharacteristicAccessType> flags) {
        return flags.contains(CharacteristicAccessType.READ);
    }

    public static boolean hasWriteAccess(Set<CharacteristicAccessType> flags) {

        return flags.contains(CharacteristicAccessType.WRITE) ||
                flags.contains(CharacteristicAccessType.WRITE_WITHOUT_RESPONSE);
    }

    public static boolean isCombined(URL url) {
        return CombinedGovernor.COMBINED_ADDRESS.equals(url.getAdapterAddress());
    }

    public static String getShortUUID(String longUUID) {
        if (longUUID.length() < 8) {
            return longUUID;
        }
        return Long.toHexString(Long.valueOf(longUUID.substring(0, 8), 16)).toUpperCase();
    }

    private static String getUID(String address) {
        return address.replaceAll("/", "").replaceAll(":", "");
    }

    private static String getAddressFromUID(String uid) {
        return uid.replaceAll(MAC_PART_REGEXP, "$1:");
    }
}
