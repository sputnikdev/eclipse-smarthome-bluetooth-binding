package org.sputnikdev.esh.binding.bluetooth.handler;


import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.manager.AdapterGovernor;
import org.sputnikdev.bluetooth.manager.AdapterListener;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.internal.AdapterConfig;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothHandlerFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class AdapterHandler extends BluetoothHandler<AdapterGovernor>
        implements BridgeHandler, AdapterListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(AdapterHandler.class);

    private final SingleChannelHandler<Boolean, OnOffType> discoveringHandler = new BooleanTypeChannelHandler(
            AdapterHandler.this, BluetoothBindingConstants.CHANNEL_DISCOVERING) {
        @Override
        Boolean getValue() {
            return getGovernor().isReady() && getGovernor().isDiscovering();
        }
    };

    private final SingleChannelHandler<Boolean, OnOffType> discoveringControlHandler = new BooleanTypeChannelHandler(
            AdapterHandler.this, BluetoothBindingConstants.CHANNEL_DISCOVERING_CONTROL, true) {
        @Override
        Boolean getValue() {
            return getGovernor().getDiscoveringControl();
        }

        @Override
        void updateThing(Boolean value) {
            getGovernor().setDiscoveringControl(value);
        }
    };

    public AdapterHandler(BluetoothHandlerFactory factory, Thing thing) {
        super(factory, thing);
        addChannelHandlers(Arrays.asList(discoveringHandler, discoveringControlHandler));
        if (thing.getLocation() == null) {
            thing.setLocation(BluetoothBindingConstants.DEFAULT_ADAPTERS_LOCATION);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        AdapterGovernor adapterGovernor = getGovernor();
        adapterGovernor.addGovernorListener(this);
        adapterGovernor.addAdapterListener(this);

        adapterGovernor.setDiscoveringControl(true);

        lastUpdatedChanged(new Date());
        updateDevice(getConfig());

        if (adapterGovernor.isReady()) {
            adapterGovernor.setAlias(thing.getLabel());
        }
        updateStatus(adapterGovernor.isReady() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    @Override
    public void dispose() {
        logger.info("Disposing Adapter handler");
        AdapterGovernor adapterGovernor = getGovernor();
        adapterGovernor.removeAdapterListener(this);
        adapterGovernor.removeGovernorListener(this);
        logger.info("Adapter handler has been disposed");
    }

    @Override
    public void powered(boolean powered) { /* do nothing for now */}

    @Override
    public void discovering(boolean discovering) {
        discoveringHandler.updateChannel(discovering);
    }

    @Override
    public void ready(boolean ready) {
        updateStatus(ready ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    @Override
    public void lastUpdatedChanged(Date lastActivity) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastActivity);
        updateState(BluetoothBindingConstants.CHANNEL_LAST_UPDATED, new DateTimeType(calendar));
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
    }

    @Override
    protected void updateDevice(Configuration configuration) {
        AdapterConfig config = configuration.as(AdapterConfig.class);

        AdapterGovernor adapterGovernor = getGovernor();

        if (config.getSignalPropagationExponent() != null) {
            adapterGovernor.setSignalPropagationExponent(config.getSignalPropagationExponent());
        }
        if (adapterGovernor.isReady()) {
            adapterGovernor.setAlias(getThing().getLabel());
        }
    }
}
