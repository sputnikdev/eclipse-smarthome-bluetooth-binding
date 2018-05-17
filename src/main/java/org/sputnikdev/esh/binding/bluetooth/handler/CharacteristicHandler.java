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
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.gattparser.spec.Enumeration;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    private ScheduledFuture<?> updateTask;
    private CompletableFuture<byte[]> authFuture;

    CharacteristicHandler(BluetoothHandler handler, URL characteristicURL, Set<CharacteristicAccessType> flags) {
        super(handler, characteristicURL, !BluetoothUtils.hasWriteAccess(flags));
        this.flags = new HashSet<>(flags);
    }

    @Override
    public void attach() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        boolean notifiable = BluetoothUtils.hasNotificationAccess(flags);
        boolean readable = BluetoothUtils.hasReadAccess(flags);
        if (notifiable) {
            characteristicGovernor.addValueListener(this);
            if (readable) {
                updateChannels();
            }
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
        if (authFuture != null) {
            authFuture.cancel(true);
        }
        CharacteristicGovernor characteristicGovernor = getGovernor();
        characteristicGovernor.removeValueListener(this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!getGovernor().isReady()) {
            handler.updateStatus(ThingStatusDetail.COMMUNICATION_ERROR,
                    "Could not update bluetooth device. Device is not ready");
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
                handler.updateStatus(ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not update bluetooth device. Error: " + ex.getMessage());
                logger.warn("Could not update bluetooth device: {} : {}", url, ex.getMessage());
            }
        }
    }

    @Override
    public void changed(byte[] value) {
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
        if (authFuture == null || authFuture.isDone()) {
            authFuture = getGovernor().whenAuthenticated(CharacteristicGovernor::read);
            authFuture.thenAccept(newData -> {
                logger.debug("Updating channels: {}", url);
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
        //TODO maybe we should check if the characteristic is authenticated?
        if (BluetoothUtils.hasWriteAccess(flags)) {
            BluetoothGattParser gattParser = handler.getParser();

            GattRequest request = gattParser.prepare(url.getCharacteristicUUID());
            try {
                updateHolder(request, fieldName, state);
                byte[] data = gattParser.serialize(request);
                if (!getGovernor().write(data)) {
                    handler.updateStatus(ThingStatusDetail.COMMUNICATION_ERROR,
                            "Could not write data to characteristic: " + url);
                }
            } catch (NumberFormatException ex) {
                logger.error("Could not parse characteristic value: {} : {}", url, state, ex);
                handler.updateStatus(ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not parse characteristic value: " + url + " : " + state);
            }
        }
    }

    private void updateThing(State state) {
        //TODO maybe we should check if the characteristic is authenticated?
        if (BluetoothUtils.hasWriteAccess(flags)) {
            BluetoothGattParser gattParser = handler.getParser();

            byte[] data = gattParser.serialize(state.toString(), 16);
            if (!getGovernor().write(data)) {
                handler.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Could not write data to characteristic: " + url);
            }
        }
    }

    private void updateHolder(GattRequest request, String fieldName, State state) {
        Field field = request.getFieldHolder(fieldName).getField();
        if (field.getFormat().isBoolean()) {
            request.setField(fieldName, convert(state, OnOffType.class) == OnOffType.ON);
        } else {
            if (field.hasEnumerations()) {
                // check if we can use enumerations
                Enumeration enumeration = getEnumeration(field, state);
                if (enumeration != null) {
                    request.setField(fieldName, enumeration);
                    return;
                }
                // fall back to simple types
            }
            if (field.getFormat().isReal()) {
                DecimalType decimalType = (DecimalType) convert(state, DecimalType.class);
                request.setField(fieldName, decimalType.longValue());
            } else if (field.getFormat().isDecimal()) {
                DecimalType decimalType = (DecimalType) convert(state, DecimalType.class);
                request.setField(fieldName, decimalType.doubleValue());
            } else if (field.getFormat().isString()) {
                StringType textType = (StringType) convert(state, StringType.class);
                request.setField(fieldName, textType.toString());
            } else if (field.getFormat().isStruct()) {
                StringType textType = (StringType) convert(state, StringType.class);
                String text = textType.toString().trim();
                if (text.startsWith("[")) {
                    request.setField(fieldName, handler.getParser().serialize(text, 16));
                } else {
                    request.setField(fieldName, new BigInteger(text));
                }
            }
        }
    }

    private Enumeration getEnumeration(Field field, State state) {
        try {
            return field.getEnumeration(new BigInteger(convert(state, DecimalType.class).toString()));
        } catch (NumberFormatException ex) {
            logger.debug("Could not not convert state to enumeration: {} : {} : {} ", url, field.getName(), state);
            return null;
        }
    }

    private static State convert(State state, Class<? extends State> typeClass) {
        return state.as(typeClass);
    }

    private CharacteristicGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getCharacteristicGovernor(url);
    }

}
