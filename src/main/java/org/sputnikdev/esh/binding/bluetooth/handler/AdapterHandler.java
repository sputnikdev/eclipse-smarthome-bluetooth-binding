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
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A bluetooth handler which represents bluetooth adapters.
 * 
 * @author Vlad Kolotov - Initial contribution
 */
public class AdapterHandler extends BluetoothHandler<AdapterGovernor>
        implements BridgeHandler, AdapterListener, GovernorListener {

    private Logger logger = LoggerFactory.getLogger(AdapterHandler.class);
    private ScheduledFuture<?> syncTask;
    private CompletableFuture<Void> setAliasFuture;

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

        @Override
        Boolean getDefaultValue() {
            return true;
        }
    };

    /**
     * Created an instance of the handler.
     * @param thing a thing
     * @param bluetoothContext bluetooth context
     */
    public AdapterHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing, bluetoothContext);
        addChannelHandlers(Arrays.asList(discoveringHandler, discoveringControlHandler));
        if (thing.getLocation() == null) {
            thing.setLocation(BluetoothBindingConstants.DEFAULT_ADAPTERS_LOCATION);
        }
    }

    @Override
    public void initialize() {
        // make sure we subscribe to events first (before channel initialization)
        AdapterGovernor adapterGovernor = getGovernor();
        adapterGovernor.addGovernorListener(this);
        adapterGovernor.addAdapterListener(this);

        // initialize all channel handlers
        super.initialize();

        lastUpdatedChanged(Instant.now());
        updateDevice(getConfig());

        setAliasFuture = adapterGovernor.<AdapterGovernor, Void>whenReady(governor -> {
            governor.setAlias(thing.getLabel());
            return null;
        });

        updateStatus(adapterGovernor.isReady() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);

        syncTask = scheduler.scheduleAtFixedRate(() -> {
            discoveringControlHandler.updateChannel(discoveringControlHandler.getValue());
        }, 5, 10, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.info("Disposing Adapter handler");
        if (syncTask != null) {
            syncTask.cancel(true);
        }
        syncTask = null;
        AdapterGovernor adapterGovernor = getGovernor();
        adapterGovernor.removeAdapterListener(this);
        adapterGovernor.removeGovernorListener(this);
        Optional.ofNullable(setAliasFuture).ifPresent(future -> future.cancel(true));
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
        if (!ready) {
            discoveringHandler.updateChannel(false);
        }
    }

    @Override
    public void lastUpdatedChanged(Instant lastActivity) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
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
