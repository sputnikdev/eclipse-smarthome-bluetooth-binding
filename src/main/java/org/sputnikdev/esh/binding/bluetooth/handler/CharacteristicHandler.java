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
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A multi-channel bluetooth device handler which represents a parsable GATT characteristic
 * where each channel is mapped to corresponding GATT field of the characteristic.
 *
 * @author Vlad Kolotov
 */
class CharacteristicHandler extends GattChannelHandler implements ValueListener {

    private Logger logger = LoggerFactory.getLogger(CharacteristicHandler.class);

    private final Set<CharacteristicAccessType> flags;
    private byte[] data;
    private ScheduledFuture<?> updateTask;
    private CompletableFuture<byte[]> readyFuture;

    CharacteristicHandler(BluetoothHandler handler, URL characteristicURL, Set<CharacteristicAccessType> flags) {
        super(handler, characteristicURL, !BluetoothUtils.hasWriteAccess(flags));
        this.flags = new HashSet<>(flags);
    }

    @Override
    public void attach() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        if (BluetoothUtils.hasNotificationAccess(flags)) {
            characteristicGovernor.addValueListener(this);
        } else if (BluetoothUtils.hasReadAccess(flags)) {
            updateChannels();
            scheduleUpdateChannels();
        }
    }

    @Override
    public void detach() {
        if (updateTask != null) {
            updateTask.cancel(true);
        }
        if (readyFuture != null) {
            readyFuture.cancel(true);
        }
        CharacteristicGovernor characteristicGovernor = getGovernor();
        characteristicGovernor.removeValueListener(this);
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
            try {
                if (binary) {
                    updateThing(state);
                } else {
                    String fieldName = getFieldName(channelUID);
                    if (fieldName != null) {
                        updateThing(fieldName, state);
                    }
                }
            } catch (Exception ex) {
                logger.warn("Could not update bluetooth device: {}", ex.getMessage());
            }
        }
    }

    @Override
    public void changed(byte[] value) {
        data = value;
        dataChanged(value, false);
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

    private void updateChannels() {
        if (readyFuture == null || readyFuture.isDone()) {
            readyFuture = getGovernor().whenReady(CharacteristicGovernor::read);
            readyFuture.thenAccept(newData -> {
                logger.debug("Updating channels: {}", url);
                data = newData;
                dataChanged(newData, false);
            }).exceptionally(ex -> {
                logger.warn("Error occurred while updating channels: {} : {}", url, ex.getMessage());
                return null;
            });
        }
    }

    private void scheduleUpdateChannels() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        int updateRate = handler.getBindingConfig().getUpdateRate();
        updateTask = handler.getScheduler().scheduleWithFixedDelay(this::updateChannels,
                updateRate, updateRate, TimeUnit.SECONDS);
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

    private void updateThing(State state) {
        if (BluetoothUtils.hasWriteAccess(flags)) {
            BluetoothGattParser gattParser = handler.getParser();

            data = gattParser.serialize(state.toString(), 16);
            if (!getGovernor().write(data)) {
                handler.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not write data to characteristic: " + url);
            }
        }
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

    private CharacteristicGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getCharacteristicGovernor(url);
    }

}
