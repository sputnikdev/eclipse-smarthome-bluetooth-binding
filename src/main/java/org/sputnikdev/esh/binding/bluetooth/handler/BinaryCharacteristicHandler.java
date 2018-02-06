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
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.bluetooth.manager.ValueListener;
import org.sputnikdev.bluetooth.manager.transport.CharacteristicAccessType;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * Binary data channel handler for GATT characteristics.
 *
 * @author Vlad Kolotov
 */
class BinaryCharacteristicHandler implements ChannelHandler, ValueListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(BinaryCharacteristicHandler.class);

    private final BluetoothHandler handler;
    private final URL url;
    private final Set<CharacteristicAccessType> flags;

    BinaryCharacteristicHandler(BluetoothHandler handler, URL characteristicURL, Set<CharacteristicAccessType> flags) {
        this.handler = handler;
        url = characteristicURL;
        this.flags = flags;
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
        handler.updateState(BluetoothUtils.getChannelUID(url),
                value != null
                        ? new StringType(handler.getBluetoothContext().getParser().parse(value, 16))
                        : UnDefType.UNDEF);
    }

    private CharacteristicGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getCharacteristicGovernor(url);
    }

    @Override
    public void ready(boolean isReady) {
        if (isReady && BluetoothUtils.hasReadAccess(flags)) {
            byte[] data = getGovernor().read();
            handler.updateState(BluetoothUtils.getChannelUID(url), new StringType(Arrays.toString(data)));
        }
    }

    private byte[] convert(StringType command) {
        if (command == null) {
            return null;
        }
        String data = command.toString();
        return handler.getBluetoothContext().getParser().serialize(data, 16);
    }
}
