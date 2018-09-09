package org.sputnikdev.esh.binding.bluetooth.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.config.discovery.inbox.Inbox;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.AddressUtils;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.AdapterDiscoveryListener;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.DeviceDiscoveryListener;
import org.sputnikdev.bluetooth.manager.DiscoveredAdapter;
import org.sputnikdev.bluetooth.manager.DiscoveredDevice;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

/**
 * Bluetooth adapters and devices discovery service.
 *
 * @author Vlad Kolotov
 */
@Component(immediate = true, service = DiscoveryService.class, name = "binding.bluetooth.discovery",
        configurationPid = "discovery.bluetooth")
public class BluetoothDiscoveryServiceImpl extends AbstractDiscoveryService
        implements DeviceDiscoveryListener, AdapterDiscoveryListener {

    public static final int DISCOVERY_RATE_SEC = 10;

    private final Logger logger = LoggerFactory.getLogger(BluetoothDiscoveryServiceImpl.class);
    private BluetoothManager bluetoothManager;
    private Inbox inbox;

    public BluetoothDiscoveryServiceImpl() {
        super(BluetoothBindingConstants.SUPPORTED_THING_TYPES, 0, false);
    }

    @Override
    public void discovered(DiscoveredDevice device) {
        if (isBackgroundDiscoveryEnabled()) {
            discover(device);
        }
    }

    @Override
    public void discovered(DiscoveredAdapter adapter) {
        if (isBackgroundDiscoveryEnabled()) {
            discover(adapter);
        }
    }

    @Override
    public void adapterLost(URL url) {
        if (isBackgroundDiscoveryEnabled()) {
            logger.info("Adapter lost: {}", url);
            thingRemoved(BluetoothUtils.getAdapterUID(url));
        }
    }

    @Override
    public void deviceLost(DiscoveredDevice device) {
        if (isBackgroundDiscoveryEnabled()) {
            logger.info("Device lost: {}", device.getURL());
            thingRemoved(BluetoothUtils.getDeviceUID(device));
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
        inbox.stream().filter(thing -> {
            ThingTypeUID thingTypeUID = thing.getThingTypeUID();
            return BluetoothBindingConstants.SUPPORTED_THING_TYPES.contains(thingTypeUID);
        }).forEach(thing -> inbox.remove(thing.getThingUID()));
    }

    @Override
    protected void startScan() {
        bluetoothManager.getDiscoveredAdapters().forEach(this::discover);
        bluetoothManager.getDiscoveredDevices().forEach(this::discover);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (this.bluetoothManager != null) {
            this.bluetoothManager.addAdapterDiscoveryListener(this);
            this.bluetoothManager.addDeviceDiscoveryListener(this);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        if (this.bluetoothManager != null) {
            this.bluetoothManager.removeAdapterDiscoveryListener(this);
            this.bluetoothManager.removeDeviceDiscoveryListener(this);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setBluetoothManager(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    protected void unsetBluetoothManager(BluetoothManager bluetoothManager) {
        if (this.bluetoothManager != null) {
            this.bluetoothManager.removeAdapterDiscoveryListener(this);
            this.bluetoothManager.removeDeviceDiscoveryListener(this);
        }
        this.bluetoothManager = null;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public void setInbox(Inbox inbox) {
        this.inbox = inbox;
    }

    public void unsetInbox(Inbox inbox) {
        this.inbox = null;
    }

    private void discover(DiscoveredDevice device) {
        ThingUID bridgeUID = device.isCombined() ? null : BluetoothUtils.getAdapterUID(device.getURL());
        ThingUID thingUID = BluetoothUtils.getDeviceUID(device);

        DiscoveryResultBuilder builder = DiscoveryResultBuilder
                .create(thingUID)
                .withLabel((device.getAlias() != null ? device.getAlias() : device.getName())
                        + " [" + AddressUtils.guessDeviceAddressType(device.getURL()) + "]")
                .withTTL(DISCOVERY_RATE_SEC * 3)
                .withRepresentationProperty(device.getURL().getDeviceAddress())
                .withBridge(bridgeUID);

        thingDiscovered(builder.build());
    }

    private void discover(DiscoveredAdapter adapter) {
        thingDiscovered(DiscoveryResultBuilder
                .create(BluetoothUtils.getAdapterUID(adapter.getURL()))
                .withLabel(adapter.getAlias() != null ? adapter.getAlias() : adapter.getName())
                .withRepresentationProperty(adapter.getURL().getAdapterAddress())
                .withTTL(DISCOVERY_RATE_SEC * 3).build());
    }

}
