package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.CharacteristicGovernor;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A multi-channel bluetooth device handler which represents a parsable advertising service data
 * where each channel is mapped to corresponding GATT field of the advertised service data
 * (a characteristic effectively).
 *
 * @author Vlad Kolotov
 */
class ServiceHandler implements ChannelHandler, BluetoothSmartDeviceListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(ServiceHandler.class);

    private final BluetoothHandler handler;
    private final URL url;

    ServiceHandler(BluetoothHandler handler, URL serviceURL) {
        this.handler = handler;
        // converting service data URL to look like it is a characteristic
        this.url = serviceURL.copyWithCharacteristic(serviceURL.getServiceUUID());
    }

    @Override
    public void init() {
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.addBluetoothSmartDeviceListener(this);
        deviceGovernor.addGovernorListener(this);

        updateChannels();
    }

    @Override
    public void dispose() {
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.removeBluetoothSmartDeviceListener(this);
        deviceGovernor.removeGovernorListener(this);
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) { }

    @Override
    public void servicesResolved(List<GattService> gattServices) { }

    @Override
    public void ready(boolean isReady) { }

    @Override
    public void serviceDataChanged(Map<URL, byte[]> serviceData) {
        if (serviceData.containsKey(url.getServiceURL())) {
            byte[] data = serviceData.get(url.getServiceURL());
            if (handler.getBluetoothContext().getParser().isKnownCharacteristic(url.getServiceUUID())) {
                Map<String, FieldHolder> holders = parseCharacteristic(data);
                for (FieldHolder holder : holders.values()) {
                    Channel channel = getChannel(holder);
                    if (channel != null) {
                        updateState(getChannel(holder), holder);
                    }
                }
            } else {
                handler.updateState(getBinaryChannel().getUID(),
                        new StringType(handler.getBluetoothContext().getParser().parse(data, 16)));
            }
        }
    }

    private void updateChannels() {
        DeviceGovernor governor = getGovernor();
        if (governor.isReady()) {
            serviceDataChanged(getGovernor().getServiceData());
        }
    }

    private Map<String, FieldHolder> parseCharacteristic(byte[] value) {
        try {
            return handler.getBluetoothContext().getParser().parse(url.getServiceUUID(), value).getHolders();
        } catch (CharacteristicFormatException ex) {
            logger.error("Could not parse service data: " + url + ". Ignoring it...", ex);
            return new LinkedHashMap<>();
        }
    }

    private void updateState(Channel channel, FieldHolder holder) {
        handler.updateState(channel.getUID(), BluetoothUtils.convert(holder));
    }

    private Channel getChannel(FieldHolder fieldHolder) {
        return handler.getThing().getChannel(
                BluetoothUtils.getChannelUID(url.copyWithField(fieldHolder.getField().getName())));
    }

    private Channel getBinaryChannel() {
        return handler.getThing().getChannel(BluetoothUtils.getChannelUID(url));
    }

    private DeviceGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getDeviceGovernor(url);
    }

}
