package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothBindingConfig;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothContext;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

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
    private final Map<ChannelHandler, Set<ChannelUID>> channelHandlers = new ConcurrentHashMap<>();
    private final Object updateLock = new Object();
    private CompletableFuture<Void> initFuture;

    BluetoothHandler(Thing thing, BluetoothContext bluetoothContext) {
        super(thing);
        this.bluetoothContext = bluetoothContext;
        url = BluetoothUtils.getURL(thing);
    }

    @Override
    public void initialize() {
        super.initialize();

        initFuture = getGovernor().whenReady(governor -> {
            initChannelHandlers();
            return null;
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        findHandler(channelUID, handler -> handler.handleCommand(channelUID, command));
    }

    @Override
    public void dispose() {
        logger.info("Disposing Abstract Bluetooth Handler");
        super.dispose();
        disposeChannelHandlers();
        Optional.ofNullable(initFuture).ifPresent(future -> future.cancel(true));
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
    public void channelLinked(ChannelUID channelUID) {
        findHandler(channelUID, ChannelHandler::linked);
        super.channelLinked(channelUID);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        super.channelUnlinked(channelUID);
        findHandler(channelUID, channelHandler -> {
            if (channelHandlers.get(channelHandler).stream()
                    .filter(uid -> isLinked(uid.getIdWithoutGroup()) && !uid.equals(channelUID))
                    .count() == 0) {
                channelHandler.unlinked();
            }
        });
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

    @Override
    protected ThingBuilder editThing() {
        return super.editThing();
    }

    @Override
    protected void updateThing(Thing thing) {
        super.updateThing(thing);
    }

    protected void updateDevice(Configuration configuration) {
        // default implementation
    }

    protected BluetoothContext getBluetoothContext() {
        return bluetoothContext;
    }

    protected void addChannelHandler(ChannelHandler channelHandler) {
        channelHandlers.computeIfAbsent(channelHandler, handler -> new HashSet<>());
    }

    protected void registerChannel(ChannelUID channelUID, ChannelHandler channelHandler) {
        channelHandlers.computeIfAbsent(channelHandler, handler -> {
            if (isLinked(channelUID.getIdWithoutGroup())) {
                channelHandler.linked();
            }
            return new HashSet<>();
        }).add(channelUID);
    }

    protected ChannelHandler findHandler(ChannelUID channelUID) {
        return channelHandlers.entrySet().stream().filter(entry -> entry.getValue().contains(channelUID))
                .findAny().map(Map.Entry::getKey).orElse(null);
    }

    protected void addChannelHandlers(List<SingleChannelHandler> handlers) {
        handlers.forEach(handler -> {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), handler.getChannelID());
            registerChannel(channelUID, handler);
        });
    }

    protected void updateThingWithChannels(List<Channel> channels) {
        if (!channels.isEmpty()) {
            logger.debug("Updating thing with channels: {} / {}", url, channels.size());
            synchronized (updateLock) {
                ThingBuilder thingBuilder = editThing();
                channels.forEach(channel -> {
                    String uid = channel.getUID().getIdWithoutGroup();
                    if (getChannel(uid) == null) {
                        logger.debug("Channel to be added: {} {}", url, uid);
                        thingBuilder.withChannel(channel);
                    }
                });
                logger.debug("Updating thing with new channels: {} / {}", url, channels.size());
                updateThing(thingBuilder.build());
            }
        }
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

    protected Channel getChannel(URL url) {
        return thing.getChannel(BluetoothUtils.getChannelUID(url));
    }

    protected Channel getChannel(String uid) {
        return thing.getChannel(uid);
    }

    protected List<Channel> getChannels() {
        return thing.getChannels();
    }

    protected ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    protected void updateStatus(ThingStatusDetail detail, String message) {
        updateStatus(getGovernor().isReady() ? ThingStatus.ONLINE : ThingStatus.OFFLINE, detail, message);
    }

    private void initChannelHandlers() {
        channelHandlers.keySet().forEach(ChannelHandler::init);
    }

    private void disposeChannelHandlers() {
        channelHandlers.keySet().forEach(ChannelHandler::dispose);
        channelHandlers.clear();
    }

    private void findHandler(ChannelUID channelUID, Consumer<ChannelHandler> consumer) {
        channelHandlers.entrySet().stream().filter(entry -> entry.getValue().contains(channelUID))
                .findAny().ifPresent(entry -> consumer.accept(entry.getKey()));
    }

}
