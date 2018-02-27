package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Binary data channel handler for GATT characteristics.
 *
 * @author Vlad Kolotov
 */
class BinaryCharacteristicHandler implements ChannelHandler, ValueListener {

    private Logger logger = LoggerFactory.getLogger(BinaryCharacteristicHandler.class);

    private final BluetoothHandler handler;
    private final URL url;
    private final Set<CharacteristicAccessType> flags;
    private ScheduledFuture<?> updateTask;
    private CompletableFuture<byte[]> readyFuture;

    BinaryCharacteristicHandler(BluetoothHandler handler, URL characteristicURL, Set<CharacteristicAccessType> flags) {
        this.handler = handler;
        url = characteristicURL;
        this.flags = flags;
    }

    @Override
    public void init() { }

    @Override
    public void linked() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        if (BluetoothUtils.hasNotificationAccess(flags)) {
            characteristicGovernor.addValueListener(this);
        } else if (BluetoothUtils.hasReadAccess(flags)) {
            scheduleUpdateChannel();
        }
        updateChannel();
    }

    @Override
    public void unlinked() {
        CharacteristicGovernor characteristicGovernor = getGovernor();
        characteristicGovernor.removeValueListener(this);
    }

    @Override
    public void dispose() {
        unlinked();
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String id = channelUID.getIdWithoutGroup();
        String characteristicID = BluetoothUtils.getChannelUID(url);
        if (id.startsWith(characteristicID)) {
            if (command instanceof StringType && BluetoothUtils.hasWriteAccess(flags)) {
                if (!getGovernor().write(convert((StringType) command))) {
                    handler.updateStatus(ThingStatus.ONLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Could not write data to characteristic: " + getURL());
                }
            }
        }
    }

    @Override
    public void changed(byte[] value) {
        updateChannel(value);
    }

    private void updateChannel(byte[] value) {
        handler.updateState(BluetoothUtils.getChannelUID(url),
                value != null
                        ? new StringType(handler.getBluetoothContext().getParser().parse(value, 16))
                        : UnDefType.UNDEF);
    }

    private void scheduleUpdateChannel() {
        if (updateTask != null) {
            updateTask.cancel(false);
        }
        int updateRate = handler.getBindingConfig().getUpdateRate();
        updateTask = handler.getScheduler().scheduleWithFixedDelay(this::updateChannel,
                updateRate, updateRate, TimeUnit.SECONDS);
    }

    private void updateChannel() {
        if (BluetoothUtils.hasReadAccess(flags) && (readyFuture == null || readyFuture.isDone())) {
            readyFuture = getGovernor().whenReady(CharacteristicGovernor::read);
            readyFuture.thenAccept(newData -> {
                logger.debug("Updating binary channel: {}", url);
                updateChannel(newData);
            }).exceptionally(ex -> {
                logger.warn("Error occurred while updating binary channel: {} : {}", url, ex.getMessage());
                return null;
            });
        }
    }

    private CharacteristicGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getCharacteristicGovernor(url);
    }

    private byte[] convert(StringType command) {
        if (command == null) {
            return null;
        }
        return handler.getBluetoothContext().getParser().serialize(command.toString(), 16);
    }
}
