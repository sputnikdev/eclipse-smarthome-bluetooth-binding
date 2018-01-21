package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.RssiKalmanFilter;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GenericBluetoothDeviceListener;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;
import org.sputnikdev.esh.binding.bluetooth.internal.DeviceConfig;

import java.util.Arrays;
import java.util.Date;

/**
 * A bluetooth handler which represents generic bluetooth devices (prior bluetooth v4.0).
 * 
 * @author Vlad Kolotov
 */
public class GenericBluetoothDeviceHandler extends BluetoothHandler<DeviceGovernor>
        implements GenericBluetoothDeviceListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(GenericBluetoothDeviceHandler.class);

    private final BooleanTypeChannelHandler onlineHandler = new BooleanTypeChannelHandler(
            this, BluetoothBindingConstants.CHANNEL_ONLINE) {
        @Override Boolean getValue() {
            return getGovernor().isOnline();
        }
    };

    private final DateTimeChannelHandler lastChangedHandler = new DateTimeChannelHandler(
            this, BluetoothBindingConstants.CHANNEL_LAST_UPDATED) {
        @Override Date getValue() {
            return getGovernor().getLastActivity();
        }
    };

    private final IntegerTypeChannelHandler rssiHandler = new IntegerTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_RSSI) {
        @Override Integer getValue() {
            if (getGovernor().isReady()) {
                int rssi = getGovernor().getRSSI();
                if (rssi != 0) {
                    return rssi;
                }
            }
            return null;
        }
    };

    private final IntegerTypeChannelHandler txPowerHandler = new IntegerTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_TX_POWER) {
        @Override Integer getValue() {
            return getGovernor().isReady() ? (int) getGovernor().getTxPower() : null;
        }
    };

    private final DoubleTypeChannelHandler estimatedDistance = new DoubleTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_ESTIMATED_DISTANCE) {
        @Override Double getValue() {
            if (getGovernor().isReady()) {
                return getGovernor().getEstimatedDistance();
            }
            return null;
        }
    };

    private final StringTypeChannelHandler adapterHandler = new StringTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_ADAPTER) {
        @Override String getValue() {
            URL location = getGovernor().getLocation();
            return location != null ? location.getAdapterAddress() : null;
        }
    };

    private final StringTypeChannelHandler locationHandler = new StringTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_LOCATION) {
        @Override String getValue() {
            URL locationURL = getGovernor().getLocation();
            String location = null;
            if (locationURL != null) {
                Thing adapterThing = thingRegistry.get(BluetoothUtils.getAdapterUID(locationURL));
                if (adapterThing != null) {
                    location = adapterThing.getLocation();
                }
            }
            return location;
        }
    };

    /**
     * Creates an instance of the handler.
     * @param thing a thing
     * @param bluetoothContext bluetooth context
     */
    public GenericBluetoothDeviceHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing, bluetoothContext);
        addChannelHandlers(Arrays.asList(onlineHandler, lastChangedHandler, rssiHandler, txPowerHandler,
                estimatedDistance, adapterHandler, locationHandler));
        if (thing.getLocation() == null) {
            thing.setLocation(BluetoothBindingConstants.DEFAULT_DEVICES_LOCATION);
        }
    }

    @Override
    public void initialize() {
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.addGenericBluetoothDeviceListener(this);
        deviceGovernor.addGovernorListener(this);
        deviceGovernor.setBlockedControl(false);

        // init channel handlers
        super.initialize();

        Configuration conf = editConfiguration();
        if (conf.get("onlineTimeout") == null) {
            conf.put("onlineTimeout", getBindingConfig().getInitialOnlineTimeout());
            updateConfiguration(conf);
        }

        updateDevice(getConfig());

        if (deviceGovernor.isReady()) {
            deviceGovernor.setAlias(thing.getLabel());
        }
        updateStatus(deviceGovernor.isOnline() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    @Override
    public void dispose() {
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.removeGenericBluetoothDeviceListener(this);
        deviceGovernor.removeGovernorListener(this);
        deviceGovernor.setConnectionControl(false);
        super.dispose();
    }

    @Override
    public void online() {
        onlineHandler.updateChannel(true);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void offline() {
        onlineHandler.updateChannel(false);
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void blocked(boolean blocked) { /* do nothing */ }

    @Override
    public void rssiChanged(short rssi) {
        rssiHandler.updateChannel((int) rssi);
        updateLocationChannels();
    }

    @Override
    public void ready(boolean ready) {
        if (!ready) {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void lastUpdatedChanged(Date lastActivity) {
        lastChangedHandler.updateChannel(lastActivity);
    }

    @Override
    protected void updateDevice(Configuration configuration) {
        DeviceConfig config = configuration.as(DeviceConfig.class);

        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.setOnlineTimeout(config.getOnlineTimeout() != null
                ? config.getOnlineTimeout() : getBindingConfig().getInitialOnlineTimeout());

        if (config.getRssiFilterType() == null) {
            deviceGovernor.setRssiFilteringEnabled(false);
        } else {
            DeviceConfig.RssiFilterType rssiFilterType =
                    DeviceConfig.RssiFilterType.valueOf(config.getRssiFilterType());
            if (rssiFilterType == DeviceConfig.RssiFilterType.NONE) {
                deviceGovernor.setRssiFilteringEnabled(false);
            } else {
                if (deviceGovernor.getRssiFilter() instanceof RssiKalmanFilter) {
                    deviceGovernor.setRssiFilteringEnabled(true);
                    RssiKalmanFilter filter = (RssiKalmanFilter) deviceGovernor.getRssiFilter();
                    filter.setProcessNoise(rssiFilterType.getProcessNoise());
                    filter.setMeasurementNoise(rssiFilterType.getMeasurmentNoise());
                } else {
                    throw new IllegalStateException("Unknow RSSI filter: " + deviceGovernor.getRssiFilter());
                }
            }
        }

        if (config.getTxPowerMeasured() != null) {
            deviceGovernor.setMeasuredTxPower((short) (int) config.getTxPowerMeasured());
        }

        if (deviceGovernor.isReady()) {
            deviceGovernor.setAlias(getThing().getLabel());
        }
    }

    protected void updateLocationChannels() {
        estimatedDistance.updateChannel(estimatedDistance.getValue());
        locationHandler.updateChannel(locationHandler.getValue());
        adapterHandler.updateChannel(adapterHandler.getValue());
    }

}
