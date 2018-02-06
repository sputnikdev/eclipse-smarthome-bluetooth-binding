package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
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
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothBindingConfig;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A root thing handler for all bluetooth handlers. Defines overall structure and provides some useful methods
 * for its successors.
 *
 * @author Vlad Kolotov
 */
class BluetoothHandler<T extends BluetoothGovernor> extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(BluetoothHandler.class);

    private final BluetoothContext bluetoothContext;
    private final URL url;
    private final List<ChannelHandler> channelHandlers = new CopyOnWriteArrayList<>();

    BluetoothHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing);
        this.bluetoothContext = bluetoothContext;
        url = BluetoothUtils.getURL(thing);
    }

    @Override
    public void initialize() {
        super.initialize();
        initChannelHandlers();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        channelHandlers.forEach(channel -> channel.handleCommand(channelUID, command));
    }

    @Override
    public void dispose() {
        logger.info("Disposing Abstract Bluetooth Handler");
        super.dispose();
        disposeChannelHandlers();
        logger.info("Disposing bluetooth object: {}", url);
        bluetoothContext.getManager().disposeDescendantGovernors(url);
        bluetoothContext.getManager().disposeGovernor(url);
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

    protected BluetoothContext getBluetoothContext() {
        return bluetoothContext;
    }

    protected List<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    protected void addChannelHandler(ChannelHandler channelHandler) {
        channelHandlers.add(channelHandler);
    }

    protected void addChannelHandlers(List<ChannelHandler> handlers) {
        channelHandlers.addAll(handlers);
    }

    protected URL getURL() {
        return url;
    }

    protected T getGovernor() {
        return (T) bluetoothContext.getManager().getGovernor(getURL());
    }


    protected BluetoothGattParser getParser() {
        return bluetoothContext.getParser();
    }

    protected BluetoothBindingConfig getBindingConfig() {
        return bluetoothContext.getConfig();
    }

    private void initChannelHandlers() {
        channelHandlers.forEach(ChannelHandler::init);
    }

    private void disposeChannelHandlers() {
        channelHandlers.forEach(ChannelHandler::dispose);
        channelHandlers.clear();
    }

}
