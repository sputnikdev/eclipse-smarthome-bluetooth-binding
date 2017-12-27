package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothHandlerFactory;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vlad Kolotov
 */
class BluetoothHandler<T extends BluetoothGovernor> extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(BluetoothHandler.class);

    private final BluetoothHandlerFactory factory;
    private final URL url;
    private final List<ChannelHandler> channelHandlers = new ArrayList<>();

    BluetoothHandler(BluetoothHandlerFactory factory, Thing thing) {
        super(thing);
        this.factory = factory;
        url = BluetoothUtils.getURL(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        initChannelHandlers();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        synchronized (channelHandlers) {
            for (ChannelHandler channelHandler : channelHandlers) {
                channelHandler.handleCommand(channelUID, command);
            }
        }
    }

    @Override
    public void dispose() {
        logger.info("Disposing Abstract Bluetooth Handler");
        super.dispose();
        logger.info("Disposing bluetooth object: {}", url);
        getBluetoothManager().disposeDescendantGovernors(url);
        getBluetoothManager().disposeGovernor(url);
        logger.info("Abstract Bluetooth Handler has been disposed");
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        validateConfigurationParameters(configurationParameters);

        // can be overridden by subclasses
        Configuration configuration = editConfiguration();
        for (Map.Entry<String, Object> configurationParmeter : configurationParameters.entrySet()) {
            configuration.put(configurationParmeter.getKey(), configurationParmeter.getValue());
        }

        updateConfiguration(configuration);
        updateDevice(configuration);
    }

    @Override
    protected void updateState(String channelID, State state) {
        super.updateState(channelID, state);
    }

    @Override
    protected void updateState(ChannelUID channelUID, State state) {
        super.updateState(channelUID, state);
    }


    @Override
    protected Configuration getConfig() {
        return super.getConfig();
    }

    @Override
    protected Configuration editConfiguration() {
        return super.editConfiguration();
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
    }

    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, String description) {
        super.updateStatus(status, statusDetail, description);
    }

    protected void updateDevice(Configuration configuration) {
        // default implementation
    }

    protected BluetoothManager getBluetoothManager() {
        return factory.getBluetoothManager();
    }

    protected BluetoothGattParser getGattParser() {
        return factory.getGattParser();
    }

    protected ItemRegistry getItemRegistry() {
        return factory.getItemRegistry();
    }

    protected ThingRegistry getThingRegistry() {
        return factory.getThingRegistry();
    }

    List<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    void addChannelHandler(ChannelHandler channelHandler) {
        synchronized (channelHandlers) {
            channelHandlers.add(channelHandler);
        }
    }

    void addChannelHandlers(List<ChannelHandler> handlers) {
        synchronized (channelHandlers) {
            channelHandlers.addAll(handlers);
        }
    }

    URL getURL() {
        return url;
    }

    protected T getGovernor() {
        return (T) getBluetoothManager().getGovernor(getURL());
    }

    private void initChannelHandlers() {
        synchronized (channelHandlers) {
            for (ChannelHandler channelHandler : channelHandlers) {
                channelHandler.init();
            }
        }
    }

}
