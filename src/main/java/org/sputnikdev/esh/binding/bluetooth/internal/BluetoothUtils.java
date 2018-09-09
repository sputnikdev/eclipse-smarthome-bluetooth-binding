package org.sputnikdev.esh.binding.bluetooth.internal;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.sputnikdev.bluetooth.AddressType;
import org.sputnikdev.bluetooth.AddressUtils;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Enumeration;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.CombinedGovernor;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        URL url = device.getURL();
        boolean bleEnabled = device.isBleEnabled();
        ThingTypeUID thingTypeUID;
        AddressType addressType = AddressUtils.guessDeviceAddressType(url);
        String address;
        if (addressType == AddressType.RESOLVABLE || addressType == AddressType.NON_RESOLVABLE) {
            thingTypeUID = BluetoothBindingConstants.THING_TYPE_BEACON;
            //TODO possibly add some service UUIDs as well
            address = encodeBeaconUID(url.copyWithDevice(null, "name", device.getName()).getDeviceCompositeAddress());
        } else {
            thingTypeUID = bleEnabled
                    ? BluetoothBindingConstants.THING_TYPE_BLE :
                    BluetoothBindingConstants.THING_TYPE_GENERIC;
            address = url.getDeviceAddress();
        }
        return new ThingUID(thingTypeUID, getUID(address));
    }

    public static URL getURL(Thing thing) {
        if (BluetoothBindingConstants.THING_TYPE_ADAPTER.equals(thing.getThingTypeUID())) {
            return new URL(getAddressFromUID(thing.getUID().getId()), null);
        } else {
            String adapterAddress = Optional.of(thing).map(Thing::getBridgeUID).map(ThingUID::getId)
                    .map(BluetoothUtils::getAddressFromUID).orElse(CombinedGovernor.COMBINED_ADDRESS);
            String device;
            if (BluetoothBindingConstants.THING_TYPE_BEACON.equals(thing.getThingTypeUID())) {
                device = decodeBeaconUID(thing.getUID().getId());
            } else {
                device = getAddressFromUID(thing.getUID().getId());
            }
            return new URL("/" + adapterAddress + "/" + device);
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

    public static State convert(BluetoothGattParser parser, FieldHolder holder) {
        State state;
        if (holder.isValueSet()) {
            if (holder.getField().getFormat().isBoolean()) {
                state = holder.getBoolean() ? OnOffType.ON : OnOffType.OFF;
            } else {
                // check if we can use enumerations
                if (holder.getField().hasEnumerations()) {
                    Enumeration enumeration = holder.getEnumeration();
                    if (enumeration != null) {
                        if (holder.getField().getFormat().isNumber()) {
                            return new DecimalType(new BigDecimal(enumeration.getKey()));
                        } else {
                            return new StringType(enumeration.getKey().toString());
                        }
                    }
                    // fall back to simple types
                }
                if (holder.getField().getFormat().isNumber()) {
                    state = new DecimalType(holder.getBigDecimal());
                } else if (holder.getField().getFormat().isStruct()) {
                    state = new StringType(parser.parse(holder.getBytes(), 16));
                } else {
                    state = new StringType(holder.getString());
                }
            }
        } else {
            state = UnDefType.UNDEF;
        }
        return state;
    }

    public static String encodeFieldID(Field field) {
        try {
            String requirements = Optional.ofNullable(field.getRequirements())
                    .orElse(Collections.emptyList()).stream().collect(Collectors.joining());
            return Base64.getEncoder().encodeToString((field.getName() + requirements)
                    .getBytes("UTF-8")).replace("=", "");
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

    private static String decodeBeaconUID(String uid) {
        try {
            return new String(DatatypeConverter.parseHexBinary(uid), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encodeBeaconUID(String uid) {
        try {
            return DatatypeConverter.printHexBinary(uid.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
