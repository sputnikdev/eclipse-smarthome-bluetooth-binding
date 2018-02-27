package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.NotReadyException;

/**
 * A base class that is used for mapping a basic governor state/control to a corresponding thing channel.
 * Bluetooth thing handlers are supposed to define a list of single channel handlers in a declarative manner
 * by extending this interface or one of the already defined abstract implementations.
 *
 * @author Vlad Kolotov
 */
abstract class SingleChannelHandler<V, S extends State> implements ChannelHandler {

    private Logger logger = LoggerFactory.getLogger(SingleChannelHandler.class);

    protected final BluetoothHandler handler;
    private final String channelID;
    private final boolean persistent;

    SingleChannelHandler(BluetoothHandler handler, String channelID) {
        this(handler, channelID, false);
    }

    SingleChannelHandler(BluetoothHandler handler, String channelID, boolean persistent) {
        this.handler = handler;
        this.channelID = channelID;
        this.persistent = persistent;
    }

    String getChannelID() {
        return channelID;
    }

    /**
     * Converts a channel state to a value which can be consumed by a bluetooth governor.
     * @param state a channel state
     * @return a value which can be consumed by a bluetooth governor
     */
    abstract V convert(S state);

    /**
     * Converts a value which is produced by a bluetooth governor to a channel state.
     * @param value a bluetooth governor value
     * @return corresponding channel state state
     */
    abstract S convert(V value);

    /**
     * Converts a channel state which is stored in the JSON storage to a corresponding governor value.
     * Only Boolean, String and BigDecimal values are supported (this is what OH returns for binding configs).
     * @param stored state
     * @return corresponding governor value
     */
    abstract V load(Object stored);

    /**
     * Reads a value from the governor that is to be used to update the corresponding thing channel.
     * @return a value from the governor
     */
    abstract V getValue();

    /**
     * Updates the device governor with a provided value.
     * Most of the governor values are read only, so the default implementation does nothing.
     * @param value a value to be updated with
     */
    void updateThing(V value) { /* do nothing */ }

    /**
     * Defines a default value if either governor or channel do not have any state yet (e.g. is null).
     * @return default value
     */
    V getDefaultValue() {
        return null;
    }

    @Override public void dispose() { /* do nothing */ }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelID.equals(channelUID.getIdWithoutGroup())) {
            if (command instanceof RefreshType) {
                updateChannel(convert(getValue()));
            } else {
                V value = convert((S) command);
                updateThing(convert((S) command));
                if (persistent) {
                    persist(value);
                }
            }
        }
    }

    @Override
    public URL getURL() {
        return handler.getURL();
    }

    @Override
    public void init() {
        try {
            if (persistent) {
                V value = getInitialValue();
                if (value != null) {
                    init(value);
                }
            } else {
                updateChannel(getValue());
            }
        } catch (NotReadyException ex) {
            logger.info("Device is not ready {}. Thing channel could not be initialised.", getURL());
        }
    }

    /**
     * Updates the corresponding channel with a governor value.
     * @param value governor value
     */
    protected void updateChannel(V value) {
        State state = convert(value);
        handler.updateState(channelID, state != null ? state : UnDefType.UNDEF);
    }

    private void init(V value) {
        updateThing(value);
        updateChannel(value);
    }

    private void updateChannel(S command) {
        handler.updateState(channelID, command != null ? command : UnDefType.UNDEF);
    }

    private void persist(V value) {
        Configuration configuration = handler.editConfiguration();
        configuration.put(this.channelID, value);
        handler.updateConfiguration(configuration);
    }

    private V getInitialValue() {
        V result = null;

        // Only Boolean, String and BigDecimal values are supported (this is what OH returns for binding configs)
        Object stored = handler.getConfig().get(channelID);
        if (stored != null) {
            result = load(stored);
        }
        if (result == null) {
            result = getDefaultValue();
        }
        return result;
    }

}
