package org.sputnikdev.esh.binding.bluetooth.internal;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.CombinedGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * An utility class which contains some common functionality for the bluetooth handlers.
 *
 * @author Vlad Kolotov
 */
public final class BluetoothUtils {

    private static final String MAC_PART_REGEXP = "(\\w{2}(?=(\\w{2})))";

    private BluetoothUtils() { }

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

    public static boolean hasNotificationAccess(Set<CharacteristicAccessType> flags) {
        return flags.contains(CharacteristicAccessType.NOTIFY)
                || flags.contains(CharacteristicAccessType.INDICATE);
    }

    public static boolean hasReadAccess(Set<CharacteristicAccessType> flags) {
        return flags.contains(CharacteristicAccessType.READ);
    }

    public static boolean hasWriteAccess(Set<CharacteristicAccessType> flags) {

        return flags.contains(CharacteristicAccessType.WRITE)
                || flags.contains(CharacteristicAccessType.WRITE_WITHOUT_RESPONSE);
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

    public static State convert(FieldHolder holder) {
        State state;
        if (holder.isValueSet()) {
            if (holder.getField().getFormat().isBoolean()) {
                state = holder.getBoolean() ? OnOffType.ON : OnOffType.OFF;
            } else if (holder.getField().getFormat().isNumber()) {
                state = new DecimalType(holder.getBigDecimal());
            } else {
                state = new StringType(holder.getString());
            }
        } else {
            state = UnDefType.UNDEF;
        }
        return state;
    }

    public static boolean hasEnums(Field field) {
        return field.getEnumerations() != null && field.getEnumerations().getEnumerations() != null;
    }

    public static String encodeFieldName(String fieldName) {
        try {
            return Base64.getEncoder().encodeToString(fieldName.getBytes("UTF-8")).replace("=", "");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String getUID(String address) {
        return address.replaceAll("/", "").replaceAll(":", "");
    }

    private static String getAddressFromUID(String uid) {
        return uid.replaceAll(MAC_PART_REGEXP, "$1:");
    }
}
