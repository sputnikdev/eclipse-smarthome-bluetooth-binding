package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private final boolean knownCharacteristic;
    private final Object updateLock = new Object();

    ServiceHandler(BluetoothHandler handler, URL serviceURL) {
        this.handler = handler;
        // converting service data URL to look like it is a characteristic
        url = serviceURL.copyWithCharacteristic(serviceURL.getServiceUUID());
        knownCharacteristic = handler.getParser().isKnownCharacteristic(url.getCharacteristicUUID());
    }

    @Override
    public void init() {
        if (!knownCharacteristic) {
            buildBinaryChannel();
        }
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.addBluetoothSmartDeviceListener(this);
        deviceGovernor.addGovernorListener(this);
    }

    protected void init(byte[] data) {
        init();
        serviceDataChanged(data);
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
            serviceDataChanged(data);
        }
    }

    ChannelUID getChannelUID() {
        return new ChannelUID(handler.getThing().getUID(), BluetoothUtils.getChannelUID(url));
    }

    private void serviceDataChanged(byte[] data) {
        if (knownCharacteristic) {
            Collection<FieldHolder> holders = parseCharacteristic(data);
            buildChannels(holders);
            holders.forEach(holder -> {
                Channel channel = getChannel(holder);
                if (channel != null) {
                    updateState(channel, holder);
                }
            });
        } else {
            updateBinaryState(data);
        }
    }

    private void buildChannels(Collection<FieldHolder> holders) {
        List<Channel> channels = new ArrayList<>();
        BluetoothChannelBuilder builder = new BluetoothChannelBuilder(handler);
        for (FieldHolder holder : holders) {
            Channel channel = getChannel(holder);
            if (channel == null) {
                Field field = holder.getField();
                if (!field.isFlagField()
                        && !field.isUnknown() || handler.getBindingConfig().isDiscoverUnknownAttributes()) {
                    channels.add(builder.buildFieldChannel(url, field, false, true));
                }
            }
        }

        if (!channels.isEmpty()) {
            synchronized (updateLock) {
                ThingBuilder thingBuilder = handler.editThing();
                channels.forEach(channel -> {
                    if (handler.getChannel(channel.getUID().getIdWithoutGroup()) == null) {
                        thingBuilder.withChannel(channel);
                    }
                });
                handler.updateThing(thingBuilder.build());
            }
        }
    }

    private void buildBinaryChannel() {
        if (handler.getBindingConfig().isDiscoverUnknownAttributes() && getBinaryChannel() == null) {
            logger.debug("Building a new binary channel for service data url: {}", url);
            ThingBuilder thingBuilder = handler.editThing();
            BluetoothChannelBuilder builder = new BluetoothChannelBuilder(handler);
            thingBuilder.withChannel(builder.buildCharacteristicBinaryChannel(url, false, true));
            handler.updateThing(thingBuilder.build());
        }
    }

    private Collection<FieldHolder> parseCharacteristic(byte[] value) {
        try {
            return handler.getParser().parse(url.getServiceUUID(), value).getFieldHolders();
        } catch (CharacteristicFormatException ex) {
            logger.error("Could not parse service data: " + url + ". Ignoring it...", ex);
            return Collections.emptyList();
        }
    }

    private void updateState(Channel channel, FieldHolder holder) {
        handler.updateState(channel.getUID(), BluetoothUtils.convert(holder));
    }

    private void updateBinaryState(byte[] data) {
        Channel channel = getBinaryChannel();
        if (channel != null) {
            handler.updateState(channel.getUID().getIdWithoutGroup(),
                    new StringType(handler.getBluetoothContext().getParser().parse(data, 16)));
        }
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
