package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.RssiKalmanFilter;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GenericBluetoothDeviceListener;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

/**
 *
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class GenericBluetoothDeviceHandler extends BluetoothHandler<DeviceGovernor>
        implements GenericBluetoothDeviceListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(GenericBluetoothDeviceHandler.class);
    private int initialOnlineTimeout;

    private final SingleChannelHandler<Boolean, OnOffType> readyHandler = new BooleanTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_READY) {
        @Override Boolean getValue() {
            return getGovernor().isReady();
        }
    };

    private final DateTimeChannelHandler lastChangedHandler =
            new DateTimeChannelHandler(this, BluetoothBindingConstants.CHANNEL_LAST_UPDATED);

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

    private final SingleChannelHandler<Boolean, OnOffType> rssiFilteringHandler = new BooleanTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_RSSI_FILTERING, true) {
        @Override Boolean getValue() {
            return getGovernor().isRssiFilteringEnabled();
        }
        @Override void updateThing(Boolean value) {
            getGovernor().setRssiFilteringEnabled(value);
        }
    };

    private final DoubleTypeChannelHandler rssiFilteringMeasurementNoiseHandler = new DoubleTypeChannelHandler(
        GenericBluetoothDeviceHandler.this,
        BluetoothBindingConstants.CHANNEL_RSSI_FILTERING_MEASUREMENT_NOISE, true) {
        @Override Double getValue() {
            return Optional.of(getGovernor()).map(governor -> (RssiKalmanFilter) governor.getRssiFilter())
                .map(RssiKalmanFilter::getMeasurementNoise).orElse(0.0);
        }
        @Override void updateThing(Double value) {
            Optional.of(getGovernor()).map(governor -> (RssiKalmanFilter) governor.getRssiFilter())
                .ifPresent(filter -> filter.setMeasurementNoise(value));
        }
    };

    private final DoubleTypeChannelHandler rssiFilteringProcessNoiseHandler = new DoubleTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_RSSI_FILTERING_PROCESS_NOISE,
        true) {
        @Override Double getValue() {
            return Optional.of(getGovernor()).map(governor -> (RssiKalmanFilter) governor.getRssiFilter())
                .map(RssiKalmanFilter::getProcessNoise).orElse(0.0);
        }
        @Override void updateThing(Double value) {
            Optional.of(getGovernor()).map(governor -> (RssiKalmanFilter) governor.getRssiFilter())
                .ifPresent(filter -> filter.setProcessNoise(value));
        }
    };

    private final IntegerTypeChannelHandler txPowerHandler = new IntegerTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_TX_POWER) {
        @Override Integer getValue() {
            return getGovernor().isReady() ? (int) getGovernor().getTxPower() : null;
        }
    };

    private final IntegerTypeChannelHandler txPowerMeasured = new IntegerTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_TX_POWER_MEASURED,
        true) {
        @Override Integer getValue() {
            return (int) getGovernor().getMeasuredTxPower();
        }
        @Override void updateThing(Integer value) {
            getGovernor().setMeasuredTxPower(value != null ? (short) value.intValue() : 0);
            estimatedDistance.updateChannel(estimatedDistance.getValue());
        }
    };

    private final DoubleTypeChannelHandler signalPropagationExponentHandler = new DoubleTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_SIGNAL_PROPAGATION_EXPONENT,
        true) {
        @Override Double getValue() {
            return getGovernor().getSignalPropagationExponent();
        }
        @Override void updateThing(Double value) {
            getGovernor().setSignalPropagationExponent(value);
            estimatedDistance.updateChannel(estimatedDistance.getValue());
        }
    };

    private final DoubleTypeChannelHandler estimatedDistance = new DoubleTypeChannelHandler(
        GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_ESTIMATED_DISTANCE) {
        @Override Double getValue() {
            if (getGovernor().isReady()) {
                double distance = getGovernor().getEstimatedDistance();
                if (distance != 0.0) {
                    return distance;
                }
            }
            return null;
        }
    };

    private final BooleanTypeChannelHandler onlineHandler = new BooleanTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_ONLINE) {
        @Override Boolean getValue() {
            return getGovernor().isOnline();
        }
    };

    private final BooleanTypeChannelHandler blockedHandler = new BooleanTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_BLOCKED) {
        @Override Boolean getValue() {
            return getGovernor().isReady() && getGovernor().isBlocked();
        }
    };

    private final SingleChannelHandler<Boolean, OnOffType> blockedControlHandler = new BooleanTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_BLOCKED_CONTROL, true) {
        @Override Boolean getValue() {
            return getGovernor().getBlockedControl();
        }
        @Override void updateThing(Boolean value) {
            getGovernor().setBlockedControl(value);
        }
    };

    private final IntegerTypeChannelHandler onlineTimeoutHandler = new IntegerTypeChannelHandler(
            GenericBluetoothDeviceHandler.this, BluetoothBindingConstants.CHANNEL_ONLINE_TIMEOUT, true) {
        @Override Integer getValue() {
            return getGovernor().getOnlineTimeout();
        }
        @Override void updateThing(Integer value) {
            getGovernor().setOnlineTimeout(value);
        }

        @Override Integer getDefaultValue() {
            return initialOnlineTimeout;
        }
    };

    public GenericBluetoothDeviceHandler(Thing thing, ItemRegistry itemRegistry,
            BluetoothManager bluetoothManager, BluetoothGattParser parser) {
        super(thing, itemRegistry, bluetoothManager, parser);
        addChannelHandlers(Arrays.asList(readyHandler, lastChangedHandler, rssiHandler, rssiFilteringHandler,
            rssiFilteringMeasurementNoiseHandler, rssiFilteringProcessNoiseHandler,
            txPowerHandler, txPowerMeasured, signalPropagationExponentHandler, estimatedDistance,
            onlineHandler, blockedHandler, blockedControlHandler, onlineTimeoutHandler));
        thing.setLocation("Bluetooth Devices");
    }

    @Override
    public void initialize() {
        super.initialize();
        getGovernor().addGenericBluetoothDeviceListener(this);
        getGovernor().addGovernorListener(this);
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        getGovernor().removeGenericBluetoothDeviceListener(this);
        getGovernor().removeGovernorListener(this);
        super.dispose();
    }

    @Override
    public void online() {
        onlineHandler.updateChannel(true);
    }

    @Override
    public void offline() {
        onlineHandler.updateChannel(false);
    }

    @Override
    public void blocked(boolean blocked) {
        blockedHandler.updateChannel(blocked);
    }

    @Override
    public void rssiChanged(short rssi) {
        rssiHandler.updateChannel((int) rssi);
        estimatedDistance.updateChannel(estimatedDistance.getValue());
    }

    @Override
    public void ready(boolean ready) {
        readyHandler.updateChannel(ready);
    }

    @Override
    public void lastUpdatedChanged(Date lastActivity) {
        lastChangedHandler.updateChannel(lastActivity);
    }

    public int getInitialOnlineTimeout() {
        return initialOnlineTimeout;
    }

    public void setInitialOnlineTimeout(int initialOnlineTimeout) {
        this.initialOnlineTimeout = initialOnlineTimeout;
    }
}
