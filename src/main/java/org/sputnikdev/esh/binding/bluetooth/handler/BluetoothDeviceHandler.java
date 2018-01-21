package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.CombinedDeviceGovernor;
import org.sputnikdev.bluetooth.manager.ConnectionStrategy;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;
import org.sputnikdev.esh.binding.bluetooth.internal.DeviceConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A bluetooth handler which represents BLE bluetooth devices (bluetooth v4.0+).
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class BluetoothDeviceHandler extends GenericBluetoothDeviceHandler
        implements BluetoothSmartDeviceListener {

    private Logger logger = LoggerFactory.getLogger(BluetoothDeviceHandler.class);
    private ScheduledFuture<?> syncTask;

    private final BooleanTypeChannelHandler connectedHandler = new BooleanTypeChannelHandler(
            BluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_CONNECTED) {
        @Override Boolean getValue() {
            return getGovernor().isReady() && getGovernor().isConnected();
        }
    };

    private final SingleChannelHandler<Boolean, OnOffType> connectionControlHandler = new BooleanTypeChannelHandler(
            BluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_CONNECTION_CONTROL, true) {
        @Override Boolean getValue() {
            return getGovernor().getConnectionControl();
        }
        @Override void updateThing(Boolean value) {
            getGovernor().setConnectionControl(value);
        }
        @Override Boolean getDefaultValue() {
            return getBluetoothContext().getConfig().isInitialConnectionControl();
        }
    };

    private final StringTypeChannelHandler connectedAdapterHandler = new StringTypeChannelHandler(
            BluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_CONNECTED_ADAPTER) {
        @Override String getValue() {
            DeviceGovernor deviceGovernor = getGovernor();
            String adapter = null;
            if (deviceGovernor instanceof CombinedDeviceGovernor) {
                URL connectedURL = ((CombinedDeviceGovernor) deviceGovernor).getConnectedAdapter();
                if (connectedURL != null) {
                    adapter = connectedURL.getAdapterAddress();
                }
            }
            return adapter;
        }
    };


    public BluetoothDeviceHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing, bluetoothContext);
        addChannelHandlers(Arrays.asList(connectedHandler, connectionControlHandler, connectedAdapterHandler));
    }

    @Override
    public void initialize() {
        super.initialize();
        getGovernor().addBluetoothSmartDeviceListener(this);

        syncTask = scheduler.scheduleAtFixedRate(() -> {
            connectionControlHandler.updateChannel(connectionControlHandler.getValue());
        }, 5, 1, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        syncTask.cancel(true);
        syncTask = null;
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.removeBluetoothSmartDeviceListener(this);
        deviceGovernor.setConnectionControl(false);
        super.dispose();
    }

    @Override
    public void connected() {
        connectedHandler.updateChannel(true);
        connectedAdapterHandler.updateChannel(connectedAdapterHandler.getValue());
        updateLocationChannels();
    }

    @Override
    public void disconnected() {
        connectedHandler.updateChannel(false);
        connectedAdapterHandler.updateChannel(connectedAdapterHandler.getValue());
    }

    @Override
    public void servicesResolved(List<GattService> gattServices) {
        ThingBuilder builder = editThing();

        logger.info("Building channels for services: {}", gattServices.size());
        Map<ChannelHandler, List<Channel>> channels =
                new BluetoothChannelBuilder(this).buildChannels(gattServices);

        for (Map.Entry<ChannelHandler, List<Channel>> entry : channels.entrySet()) {
            ChannelHandler channelHandler = entry.getKey();
            addChannelHandler(channelHandler);
            updateChannels(builder, entry.getValue());
        }

        logger.info("Updating the thing with new channels");
        updateThing(builder.build());

        for (ChannelHandler channelHandler : channels.keySet()) {
            try {
                channelHandler.init();
            } catch (Exception ex) {
                logger.error("Could not update channel handler: {}", channelHandler.getURL(), ex);
            }
        }
    }

    @Override
    public void servicesUnresolved() {
        getChannelHandlers().stream().filter(handler -> handler instanceof MultiChannelHandler)
                .forEach(ChannelHandler::dispose);
    }

    @Override
    protected void updateDevice(Configuration configuration) {
        super.updateDevice(configuration);
        DeviceConfig config = configuration.as(DeviceConfig.class);

        //TODO decide if it is time to split this class into two: simple handler and combined hendler
        DeviceGovernor deviceGovernor = getGovernor();
        if (deviceGovernor instanceof CombinedDeviceGovernor) {
            CombinedDeviceGovernor combinedDeviceGovernor = (CombinedDeviceGovernor) deviceGovernor;
            combinedDeviceGovernor.setConnectionStrategy(ConnectionStrategy.valueOf(config.getConnectionStrategy()));
            combinedDeviceGovernor.setPreferredAdapter(
                    config.getPreferredBluetoothAdapter() != null
                            ? new URL(config.getPreferredBluetoothAdapter(), null) : null);
        }
    }

    private void updateChannels(ThingBuilder builder, Collection<Channel> channels) {
        for (Channel channel : channels) {
            if (getThing().getChannel(channel.getUID().getIdWithoutGroup()) == null) {
                builder.withChannel(channel);
            }
        }
    }

}
