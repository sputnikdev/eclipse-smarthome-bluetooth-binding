package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A multi-channel bluetooth device handler which represents a parsable GATT characteristic
 * where each channel is mapped to corresponding GATT field of the characteristic.
 *
 * @author Vlad Kolotov
 */
class CharacteristicHandler implements ChannelHandler, ValueListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(CharacteristicHandler.class);

    private final BluetoothHandler handler;
    private final URL url;
    private final Set<CharacteristicAccessType> flags;
    private byte[] data;

    CharacteristicHandler(BluetoothHandler handler, URL characteristicURL, Set<CharacteristicAccessType> flags) {
        this.handler = handler;
        this.url = characteristicURL;
        this.flags = new HashSet<>(flags);
    }

    @Override
    public void init() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        characteristicGovernor.addGovernorListener(this);
        if (BluetoothUtils.hasNotificationAccess(flags)) {
            characteristicGovernor.addValueListener(this);
        }
    }

    @Override
    public void dispose() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        characteristicGovernor.removeGovernorListener(this);
        characteristicGovernor.removeValueListener(this);
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof State) {
            State state = (State) command;
            String fieldID = channelUID.getIdWithoutGroup();
            String characteristicID = BluetoothUtils.getChannelUID(url);
            if (fieldID.startsWith(characteristicID)) {
                Channel fieldChannel = handler.getThing().getChannel(channelUID.getIdWithoutGroup());
                if (fieldChannel != null) {
                    String fieldName = fieldChannel.getProperties().get(BluetoothBindingConstants.PROPERTY_FIELD_NAME);
                    updateThing(fieldName, state);
                }
            }
        }
    }

    @Override
    public void changed(byte[] value) {
        data = value;
        Map<String, FieldHolder> holders = parseCharacteristic(data);
        for (FieldHolder holder : holders.values()) {
            updateState(getChannel(holder), holder);
        }
    }

    @Override
    public void ready(boolean isReady) {
        if (isReady) {
            updateChannels();
        }
    }

    @Override
    public void lastUpdatedChanged(Date lastActivity) { /* do nothing */ }

    private void updateChannels() {
        CharacteristicGovernor governor = getGovernor();
        if (governor.isReadable()) {
            data = getGovernor().read();
            Map<String, FieldHolder> holders = parseCharacteristic(data);
            for (FieldHolder holder : holders.values()) {
                updateState(getChannel(holder), holder);
            }
        }
    }

    private void updateThing(String fieldName, State state) {
        if (BluetoothUtils.hasWriteAccess(flags)) {
            BluetoothGattParser gattParser = handler.getBluetoothContext().getParser();

            if (data == null && BluetoothUtils.hasReadAccess(flags)) {
                data = getGovernor().read();
            }

            GattRequest request;
            if (data == null) {
                request = gattParser.prepare(url.getCharacteristicUUID());
            } else {
                request = gattParser.prepare(url.getCharacteristicUUID(), data);
            }

            updateHolder(request.getFieldHolder(fieldName), state);
            data = gattParser.serialize(request);
            if (!getGovernor().write(data)) {
                handler.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not write data to characteristic: " + url);
            }
        }
    }

    private Map<String, FieldHolder> parseCharacteristic() {
        byte[] value = getGovernor().read();
        return parseCharacteristic(value);
    }

    private Map<String, FieldHolder> parseCharacteristic(byte[] value) {
        try {
            return handler.getBluetoothContext().getParser().parse(url.getCharacteristicUUID(), value).getHolders();
        } catch (CharacteristicFormatException ex) {
            logger.error("Could not parse characteristic: " + url + ". Ignoring it...", ex);
            return new LinkedHashMap<>();
        }
    }

    private void updateState(Channel channel, FieldHolder holder) {
        State state;
        if (holder.isValueSet()) {
            if (holder.getField().getFormat().isBoolean()) {
                state = holder.getBoolean() ? OnOffType.ON : OnOffType.OFF;
            } else if (holder.getField().getFormat().isNumber()) {
                state = new DecimalType(holder.getString());
            } else {
                state = new StringType(holder.getString());
            }
        } else {
            state = UnDefType.UNDEF;
        }
        handler.updateState(channel.getUID(), state);
    }

    private static void updateHolder(FieldHolder holder, State state) {
        if (holder.getField().getFormat().isBoolean()) {
            holder.setBoolean(convert(state, OnOffType.class) == OnOffType.ON);
        } else if (holder.getField().getFormat().isReal()) {
            DecimalType decimalType = (DecimalType) convert(state, DecimalType.class);
            holder.setInteger(decimalType.intValue());
        } else if (holder.getField().getFormat().isDecimal()) {
            DecimalType decimalType = (DecimalType) convert(state, DecimalType.class);
            holder.setDouble(decimalType.doubleValue());
        } else if (holder.getField().getFormat().isString()) {
            StringType textType = (StringType) convert(state, StringType.class);
            holder.setString(textType.toString());
        }
    }

    private static State convert(State state, Class<? extends State> typeClass) {
        return state.as(typeClass);
    }

    private Channel getChannel(FieldHolder fieldHolder) {
        return handler.getThing().getChannel(
                BluetoothUtils.getChannelUID(url.copyWithField(fieldHolder.getField().getName())));
    }

    private CharacteristicGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getCharacteristicGovernor(url);
    }

}
