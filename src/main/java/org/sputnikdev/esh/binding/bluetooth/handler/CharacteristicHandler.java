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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A multi-channel bluetooth device handler which represents a parsable GATT characteristic
 * where each channel is mapped to corresponding GATT field of the characteristic.
 *
 * @author Vlad Kolotov
 */
class CharacteristicHandler implements ChannelHandler, ValueListener {

    private Logger logger = LoggerFactory.getLogger(CharacteristicHandler.class);

    private final BluetoothHandler handler;
    private final URL url;
    private final Set<CharacteristicAccessType> flags;
    private byte[] data;
    private ScheduledFuture<?> updateTask;

    CharacteristicHandler(BluetoothHandler handler, URL characteristicURL, Set<CharacteristicAccessType> flags) {
        this.handler = handler;
        this.url = characteristicURL;
        this.flags = new HashSet<>(flags);
    }

    @Override
    public void init() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        if (BluetoothUtils.hasNotificationAccess(flags)) {
            characteristicGovernor.addValueListener(this);
            updateChannels(true);
        } else if (BluetoothUtils.hasReadAccess(flags)) {
            updateChannels(false);
        }
    }

    @Override
    public void dispose() {
        if (updateTask != null) {
            updateTask.cancel(true);
        }
        CharacteristicGovernor characteristicGovernor = getGovernor();
        characteristicGovernor.removeValueListener(this);
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!getGovernor().isReady()) {
            //TODO set status
            //handler.updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.COMMUNICATION_ERROR, "Device is not ready");
            return;
        }
        if (command instanceof State) {

            State state = (State) command;
            String fieldName = getFieldName(channelUID);
            if (fieldName != null) {
                try {
                    updateThing(fieldName, state);
                } catch (Exception ex) {
                    logger.warn("Could not update bluetooth device: {}", ex.getMessage());
                }
            }
        }
    }

    private String getFieldName(ChannelUID channelUID) {
        String fieldID = channelUID.getIdWithoutGroup();
        String characteristicID = BluetoothUtils.getChannelUID(url);
        if (fieldID.startsWith(characteristicID)) {
            Channel fieldChannel = handler.getThing().getChannel(channelUID.getIdWithoutGroup());
            if (fieldChannel != null) {
                return fieldChannel.getProperties().get(BluetoothBindingConstants.PROPERTY_FIELD_NAME);
            }
        }
        return null;
    }

    @Override
    public void changed(byte[] value) {
        data = value;
        Map<String, FieldHolder> holders = parseCharacteristic(data);
        for (FieldHolder holder : holders.values()) {
            updateState(getChannel(holder), holder);
        }
    }

    private void updateChannelsContinuously() {
        updateChannels(false);
    }

    private void updateChannels(boolean oneOff) {
        if (BluetoothUtils.hasReadAccess(flags)) {
            getGovernor().whenReady(CharacteristicGovernor::read)
                    .thenAccept(newData -> {
                        if (!oneOff) {
                            scheduleUpdateChannels();
                        }
                        logger.debug("Updating channels: {}", url);
                        data = newData;
                        Map<String, FieldHolder> holders = parseCharacteristic(data);
                        for (FieldHolder holder : holders.values()) {
                            updateState(getChannel(holder), holder);
                        }
                    }).exceptionally(ex -> {
                        logger.warn("Error occurred while updating channels: {}", url, ex);
                        return null;
                    });
        }
    }

    private void scheduleUpdateChannels() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        updateTask = handler.getScheduler().schedule(this::updateChannelsContinuously,
                handler.getBindingConfig().getUpdateRate(), TimeUnit.SECONDS);
    }

    private void updateThing(String fieldName, State state) {
        if (BluetoothUtils.hasWriteAccess(flags)) {
            BluetoothGattParser gattParser = handler.getParser();

            if (data == null && BluetoothUtils.hasReadAccess(flags)) {
                data = getGovernor().read();
            }

            GattRequest request;
            if (data == null || data.length == 0) {
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

    private Map<String, FieldHolder> parseCharacteristic(byte[] value) {
        if (value.length == 0) {
            logger.warn("Characteristic value is empty: {}", url);
            return Collections.emptyMap();
        }
        try {
            return handler.getBluetoothContext().getParser().parse(url.getCharacteristicUUID(), value).getHolders();
        } catch (CharacteristicFormatException ex) {
            logger.warn("Could not parse characteristic: {}. Ignoring it: {}", url, ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private void updateState(Channel channel, FieldHolder holder) {
        handler.updateState(channel.getUID(), BluetoothUtils.convert(holder));
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
