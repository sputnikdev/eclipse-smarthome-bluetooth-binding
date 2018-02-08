package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.spec.Characteristic;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.GattCharacteristic;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A utility class for creating dynamic channels for discovered GATT characteristics.
 *
 * @author Vlad Kolotov
 */
class BluetoothChannelBuilder {

    private Logger logger = LoggerFactory.getLogger(BluetoothChannelBuilder.class);

    private final BluetoothHandler handler;

    BluetoothChannelBuilder(BluetoothHandler handler) {
        this.handler = handler;
    }

    protected Map<ChannelHandler, List<Channel>> buildServicesChannels(Set<URL> urls) {
        Map<ChannelHandler, List<Channel>> result = new HashMap<>();

        for (URL url : urls) {
            logger.info("Handling a new service data url: {}", url);
            // converting service data URL to look like it is a characteristic
            URL virtualURL = url.copyWithCharacteristic(url.getServiceUUID());
            boolean isKnownCharacteristic = handler.getBluetoothContext().getParser().isKnownCharacteristic(
                    virtualURL.getCharacteristicUUID());
            if (!handler.getBindingConfig().isDiscoverUnknownAttributes() && !isKnownCharacteristic) {
                logger.info("Skipping unknown service data: {} ", virtualURL);
                continue;
            }

            logger.info("Creating channels for service data: {}", virtualURL);

            if (isKnownCharacteristic) {
                result.put(new ServiceHandler(handler, virtualURL), buildServiceChannels(virtualURL));
            } else {
                result.put(new ServiceHandler(handler, virtualURL),
                        Arrays.asList(buildCharacteristicBinaryChannel(virtualURL, false, true)));
            }
        }

        return result;
    }

    protected Map<ChannelHandler, List<Channel>> buildCharacteristicsChannels(List<GattService> services) {
        Map<ChannelHandler, List<Channel>> result = new HashMap<>();
        List<String> advancedServices = handler.getBindingConfig().getAdvancedGattServices();
        for (GattService service : services) {
            boolean advanced = advancedServices != null && advancedServices.contains(service.getURL().getServiceUUID());
            for (GattCharacteristic characteristic : service.getCharacteristics()) {
                logger.info("Handling a new characteristic: {}", characteristic.getURL());
                result.putAll(buildChannels(characteristic, advanced));
            }
        }
        return result;
    }

    private List<Channel> buildServiceChannels(URL url) {
        List<Channel> channels = new ArrayList<>();
        for (Field field : handler.getParser().getFields(url.getCharacteristicUUID())) {
            if (!field.isFlagField()
                    && (!field.isUnknown() || handler.getBindingConfig().isDiscoverUnknownAttributes())) {
                channels.add(buildFieldChannel(url, field, false, true));
            }
        }
        return channels;
    }

    private Map<ChannelHandler, List<Channel>> buildChannels(GattCharacteristic characteristic, boolean advanced) {
        Map<ChannelHandler, List<Channel>> result = new HashMap<>();
        boolean isKnownCharacteristic = handler.getBluetoothContext().getParser().isKnownCharacteristic(
                characteristic.getURL().getCharacteristicUUID());
        if (!handler.getBindingConfig().isDiscoverUnknownAttributes() && !isKnownCharacteristic) {
            logger.info("Skipping unknown characteristic: {} ", characteristic.getURL());
            return Collections.emptyMap();
        }

        Set<CharacteristicAccessType> flags = characteristic.getFlags();
        boolean readAccess = BluetoothUtils.hasReadAccess(flags)
                || BluetoothUtils.hasNotificationAccess(flags);
        boolean writeAccess = BluetoothUtils.hasWriteAccess(flags);

        if (!readAccess && !writeAccess) {
            logger.info("The characteristic {} is not supported as it cannot be read, subscribed for "
                            + "notification or written; flags: {} ", characteristic.getURL(),
                    characteristic.getFlags());
            return Collections.emptyMap();
        }

        if (isKnownCharacteristic) {
            logger.info("Creating channels for characteristic: {}", characteristic.getURL());
            result.put(new CharacteristicHandler(handler, characteristic.getURL(), flags),
                    buildChannels(characteristic.getURL(), advanced, !writeAccess));
        } else {
            result.put(new BinaryCharacteristicHandler(handler, characteristic.getURL(), flags),
                    Collections.singletonList(buildCharacteristicBinaryChannel(
                            characteristic.getURL(), advanced, !writeAccess)));
        }
        return result;
    }

    private List<Channel> buildChannels(URL characteristicURL, boolean advanced, boolean readOnly) {
        List<Channel> channels = new ArrayList<>();
        for (Field field : handler.getParser().getFields(characteristicURL.getCharacteristicUUID())) {
            if (!field.isFlagField()
                    && !field.isUnknown() || handler.getBindingConfig().isDiscoverUnknownAttributes()) {
                channels.add(buildFieldChannel(characteristicURL, field, advanced, readOnly));
            }
        }
        return channels;
    }

    protected Channel buildCharacteristicBinaryChannel(URL characteristicURL, boolean advanced, boolean readOnly) {
        ChannelUID channelUID = new ChannelUID(handler.getThing().getUID(),
                BluetoothUtils.getChannelUID(characteristicURL));

        String channelType = getChannelType(advanced, readOnly);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BluetoothBindingConstants.BINDING_ID, channelType);
        return ChannelBuilder.create(channelUID, "String")
                .withType(channelTypeUID)
                .withLabel(characteristicURL.getCharacteristicUUID())
                .build();
    }

    protected Channel buildFieldChannel(URL characteristicURL, Field field, boolean advanced, boolean readOnly) {
        URL channelURL = characteristicURL.copyWithField(field.getName());
        ChannelUID channelUID = new ChannelUID(handler.getThing().getUID(), BluetoothUtils.getChannelUID(channelURL));

        String channelType = getChannelType(advanced, readOnly);

        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BluetoothBindingConstants.BINDING_ID, channelType);
        return ChannelBuilder.create(channelUID, getAcceptedItemType(field))
                .withType(channelTypeUID)
                .withProperties(getFieldProperties(field))
                .withLabel(getChannelLabel(channelURL.getCharacteristicUUID(), field))
                .build();
    }

    private String getChannelType(boolean advanced, boolean readOnly) {
        // making channel type that should match one of channel types from the "thing-types.xml" config file, these are:
        // characteristic-advanced-readonly-field
        // characteristic-advanced-editable-field
        // characteristic-readonly-field
        // characteristic-editable-field

        return String.format("characteristic%s%s-field",
                advanced ? "-advanced" : "", readOnly ? "-readonly" : "-editable");
    }

    private Map<String, String> getFieldProperties(Field field) {
        Map<String, String> properties = new HashMap<>();
        properties.put(BluetoothBindingConstants.PROPERTY_FIELD_NAME, field.getName());
        return properties;
    }

    private String getChannelLabel(String characteristicUUID, Field field) {
        Characteristic spec = handler.getParser().getCharacteristic(characteristicUUID);
        if (spec.getValue().getFields().size() > 1) {
            return spec.getName() + "/" + field.getName();
        } else {
            return spec.getName();
        }
    }

    private String getAcceptedItemType(Field field) {
        switch (field.getFormat().getType()) {
            case BOOLEAN: return "Switch";
            case UINT:
            case SINT:
            case FLOAT_IEE754:
            case FLOAT_IEE11073: return "Number";
            case UTF8S:
            case UTF16S: return "String";
            case STRUCT: return "Binary";
            default: throw new IllegalStateException("Unknown field format type");
        }
    }

}
