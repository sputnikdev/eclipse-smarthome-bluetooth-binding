package org.sputnikdev.esh.binding.bluetooth.internal;

import org.eclipse.smarthome.config.core.ConfigOptionProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * {@link ConfigOptionProvider} that provides channel types for dynamically discovered characteristics.
 *
 * @author Vlad Kolotov
 */
@Component(immediate = true, service = ChannelTypeProvider.class)
public class CharacteristicChannelTypeProvider implements ChannelTypeProvider {

    private Logger logger = LoggerFactory.getLogger(CharacteristicChannelTypeProvider.class);

    private final Map<ChannelTypeUID, ChannelType> cache = new ConcurrentHashMap<>();

    private BluetoothGattParser gattParser;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setBluetoothGattParser(BluetoothGattParser gattParser) {
        this.gattParser = gattParser;
    }

    public void unsetBluetoothGattParser(BluetoothGattParser gattParser) {
        this.gattParser = null;
    }

    @Override
    public Collection<ChannelType> getChannelTypes(Locale locale) {
        return Collections.emptyList();
    }

    @Override
    public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
        if (channelTypeUID.getBindingId().equals(BluetoothBindingConstants.BINDING_ID)) {
            if (channelTypeUID.getId().startsWith(BluetoothBindingConstants.CHANNEL_CHARACTERISTIC)) {
                return cache.computeIfAbsent(channelTypeUID, uid -> {
                    return buildChannelType(channelTypeUID);
                });
            }
        }
        return null;
    }

    /**
     * Builds a new channel type for a channel type UID.
     * See {@link org.sputnikdev.esh.binding.bluetooth.handler.BluetoothChannelBuilder#buildChannels(URL, List, boolean, boolean)}
     * @param channelTypeUID channel type UID
     * @return new channel type
     */
    private ChannelType buildChannelType(ChannelTypeUID channelTypeUID) {
        // characteristic-advncd-readable-00002a04-0000-1000-8000-00805f9b34fb-Battery_Level
        String channelID = channelTypeUID.getId();
        boolean advanced = "advncd".equals(channelID.substring(15, 21));
        boolean readOnly = "readable".equals(channelID.substring(22, 30));
        String characteristicUUID = channelID.substring(31, 67);
        String fieldName = channelID.substring(68, channelID.length()).replace("_", " ");

        if (gattParser.isKnownCharacteristic(characteristicUUID)) {
            Map<String, List<Field>> fieldsMapping = gattParser.getFields(characteristicUUID).stream()
                    .collect(Collectors.groupingBy(BluetoothUtils::encodeFieldID));

            List<Field> fields = fieldsMapping.get(fieldName);
            if (fields.size() > 1) {
                logger.warn("Multiple fields with the same name found: {} / {}. Skipping them.",
                        characteristicUUID, fieldName);
                return null;
            }

            Field field = fields.get(0);

            List<StateOption> options = getStateOptions(field);

            String itemType = getItemType(field);

            if (itemType.equals("Switch") || itemType.equals("Binary")) {
                options = Collections.emptyList();
            }

            String format = getFormat(field);
            String unit = getUnit(field);
            String pattern = format + " " + (unit != null ? unit : "");

            return new ChannelType(
                    new ChannelTypeUID(channelTypeUID.getAsString()), advanced, itemType, field.getName(),
                    field.getInformativeText(), null, null,
                    new StateDescription(getMinimum(field), getMaximum(field),
                            null, pattern, readOnly, options), null);
        }
        return null;
    }

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        return null;
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        return null;
    }

    private static List<StateOption> getStateOptions(Field field) {
        return hasEnums(field)
                ? field.getEnumerations().getEnumerations().stream()
                    .map(enumeration -> new StateOption(String.valueOf(enumeration.getKey()), enumeration.getValue()))
                    .collect(Collectors.toList())
                : null;
    }

    private static BigDecimal getMinimum(Field field) {
        return field.getMinimum() != null ? BigDecimal.valueOf(field.getMinimum()) : null;
    }

    private static BigDecimal getMaximum(Field field) {
        return field.getMaximum() != null ? BigDecimal.valueOf(field.getMaximum()) : null;
    }

    private static boolean hasEnums(Field field) {
        return field.getEnumerations() != null && field.getEnumerations().getEnumerations() != null;
    }

    private String getItemType(Field field) {
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

    private static String getFormat(Field field) {
        String format = "%s";
        Integer decimalExponent = field.getDecimalExponent();
        if (field.getFormat().isReal() && decimalExponent != null && decimalExponent < 0) {
            format = "%." + Math.abs(decimalExponent) + "f";
        }
        return format;
    }

    private static String getUnit(Field field) {
        //TODO move this to GattParser
        String gattUnit = field.getUnit();
        if (gattUnit != null) {
            switch (gattUnit) {
                case "org.bluetooth.unit.thermodynamic_temperature.degree_celsius": return "°C";
                case "org.bluetooth.unit.electric_conductance.siemens": return "µS/cm";
                case "org.bluetooth.unit.percentage": return "%%";
                case "org.bluetooth.unit.illuminance.lux": return "lux";
                case "org.bluetooth.unit.mass.pound": return "lb";
                case "org.bluetooth.unit.mass.kilogram": return "kg";
                case "org.bluetooth.unit.length.meter": return "m";
                case "org.bluetooth.unit.length.inch": return "in";
                default: return null;
            }
        }
        return null;
    }
}
