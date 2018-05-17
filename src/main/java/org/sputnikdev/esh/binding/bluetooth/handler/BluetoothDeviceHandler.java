package org.sputnikdev.esh.binding.bluetooth.handler;

import com.google.common.collect.Sets;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.spec.Service;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.CombinedDeviceGovernor;
import org.sputnikdev.bluetooth.manager.ConnectionStrategy;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;
import org.sputnikdev.esh.binding.bluetooth.internal.DeviceConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A bluetooth handler which represents BLE bluetooth devices (bluetooth v4.0+).
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class BluetoothDeviceHandler extends GenericBluetoothDeviceHandler
        implements BluetoothSmartDeviceListener {

    private Logger logger = LoggerFactory.getLogger(BluetoothDeviceHandler.class);
    private ScheduledFuture<?> syncTask;
    private final Set<URL> advertisedServices = new HashSet<>();
    private final ReentrantLock advertisedServicesLock = new ReentrantLock();
    private final ReentrantLock serviceResolvedLock = new ReentrantLock();

    private final BooleanTypeChannelHandler connectedHandler = new BooleanTypeChannelHandler(
            BluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_CONNECTED) {
        @Override Boolean getValue() {
            return getGovernor().isReady() && getGovernor().isServicesResolved();
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

    private final BooleanTypeChannelHandler authHandler = new BooleanTypeChannelHandler(
            BluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_AUTHENTICATED) {
        @Override Boolean getValue() {
            return getGovernor().isReady() && getGovernor().isAuthenticated();
        }
    };


    public BluetoothDeviceHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing, bluetoothContext);
        addChannelHandlers(Arrays.asList(connectedHandler, connectionControlHandler, connectedAdapterHandler, authHandler));
    }

    @Override
    public void initialize() {
        DeviceGovernor governor = getGovernor();
        governor.addBluetoothSmartDeviceListener(this);

        super.initialize();

        syncTask = scheduler.scheduleAtFixedRate(() -> {
            connectionControlHandler.updateChannel(connectionControlHandler.getValue());
        }, 5, 30, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        if (syncTask != null) {
            syncTask.cancel(true);
        }
        syncTask = null;
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.removeBluetoothSmartDeviceListener(this);
        deviceGovernor.setConnectionControl(false);
        deviceGovernor.setAuthenticationProvider(null);
        super.dispose();
    }

    @Override
    public void servicesUnresolved() {
        connectedHandler.updateChannel(false);
        connectedAdapterHandler.updateChannel(null);
        authHandler.updateChannel(false);
    }

    @Override
    public void servicesResolved(List<GattService> gattServices) {
        updateConnectedHandlers();
        updateLocationHandlers();
    }

    @Override
    public void authenticated() {
        if (serviceResolvedLock.tryLock()) {
            try {
                buildChannels(getGovernor().getResolvedServices());
            } finally {
                serviceResolvedLock.unlock();
            }
        }
        authHandler.updateChannel(true);
    }

    @Override
    public void authenticationFailure(Exception reason) {
        authHandler.updateChannel(false);
    }

    @Override
    public void ready(boolean ready) {
        super.ready(ready);
        authHandler.updateChannel(false);
    }

    @Override
    public void serviceDataChanged(Map<URL, byte[]> serviceData) {
        Set<URL> channelsToBuild = Sets.difference(serviceData.keySet(), advertisedServices);
        if (!channelsToBuild.isEmpty()) {
            if (advertisedServicesLock.tryLock()) {
                try {
                    serviceData.entrySet().stream()
                            .filter(entry -> checkServiceDataHandlerNeeded(entry.getKey())).forEach(entry -> {
                                buildServiceHandler(entry.getKey(), entry.getValue());
                            });
                    advertisedServices.addAll(channelsToBuild);
                } finally {
                    advertisedServicesLock.unlock();
                }
            }
        }
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

        DeviceConfig.AuthenticationStrategy authenticationStrategy =
                DeviceConfig.AuthenticationStrategy.valueOf(config.getAuthenticationStrategy());
        switch (authenticationStrategy) {
            case AUTO: deviceGovernor.setAuthenticationProvider(new AutomaticPinCodeAuthProvider(getParser())); break;
            case PIN_CODE: deviceGovernor.setAuthenticationProvider(new ExplicitPinCodeAuthProvider(getParser(),
                    config.getPinCodeDefinition())); break;
        }

    }

    protected void buildChannels(List<GattService> gattServices) {
        logger.info("Building channels for services: {}", gattServices.size());
        List<Channel> channels = new ArrayList<>();
        gattServices.stream()
                .flatMap(service -> service.getCharacteristics().stream())
                .filter(characteristic -> checkCharacteristicHandlerNeeded(characteristic.getURL()))
                .map(characteristic -> new CharacteristicHandler(
                        this, characteristic.getURL(), characteristic.getFlags()))
                .forEach(handler -> {
                    List<Channel> chnnls = handler.buildChannels();
                    channels.addAll(chnnls);
                    chnnls.forEach(channel -> registerChannel(channel.getUID(), handler));
                });
        checkDuplicateChannelLabels(channels);
        updateThingWithChannels(channels);
    }

    private void checkDuplicateChannelLabels(List<Channel> channels) {
        channels.stream().collect(Collectors.groupingBy(Channel::getLabel))
                .values().stream().filter(list -> list.size() > 1).flatMap(List::stream).forEach(channel -> {
            String serviceUUID = channel.getProperties().get(BluetoothBindingConstants.PROPERTY_SERVICE_UUID);
            if (serviceUUID != null) {
                Service service = getParser().getService(serviceUUID);
                channels.remove(channel);
                channels.add(ChannelBuilder.create(channel.getUID(), channel.getAcceptedItemType())
                        .withType(channel.getChannelTypeUID())
                        .withProperties(channel.getProperties())
                        .withLabel(service.getName() + " / " + channel.getLabel())
                        .build());
            }
        });
    }

    private boolean checkCharacteristicHandlerNeeded(URL url) {
        return !getBindingConfig().getAdvancedGattServices().contains(url.getServiceUUID().toLowerCase())
                && (getParser().isKnownCharacteristic(url.getCharacteristicUUID())
                        || getBindingConfig().discoverUnknown());
    }

    private boolean checkServiceDataHandlerNeeded(URL url) {
        return !getBindingConfig().getAdvancedGattServices().contains(url.getServiceUUID().toLowerCase())
                && !advertisedServices.contains(url)
                && (getParser().isKnownCharacteristic(url.getServiceUUID()) || getBindingConfig().discoverUnknown());
    }

    private void buildServiceHandler(URL url, byte[] data) {
        logger.debug("Building a new handler for service data url: {}", url);
        URL virtualURL = url.copyWithCharacteristic(url.getServiceUUID());
        ServiceHandler serviceHandler = new ServiceHandler(this, virtualURL);
        serviceHandler.dataChanged(data, true);
    }

    private void updateConnectedHandlers() {
        connectedHandler.updateChannel(true);
        connectedAdapterHandler.updateChannel(connectedAdapterHandler.getValue());
    }

}
