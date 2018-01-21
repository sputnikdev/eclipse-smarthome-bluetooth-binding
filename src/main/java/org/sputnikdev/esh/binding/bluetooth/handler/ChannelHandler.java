package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.sputnikdev.bluetooth.URL;

/**
 * A interface for channel handlers which are used to map thing channels to bluetooth governor states/controls
 * and vise versa. A chanel handler is a mediator between a bluetooth governor and a thing channel,
 * Likewise {@link ThingHandler} is a high level mediator between a physical device and SmartHome, a channel handler is
 * a more concrete mediator between bluetooth governor state and a thing channel.
 *
 * @author Vlad Kolotov
 */
public interface ChannelHandler {

    /**
     * Returns governor URL.
     * @return governor URL
     */
    URL getURL();

    /**
     * Performs initialization. It is normally called when a thing handler gets initialized/created.
     * This method is used to perform initialization of the thing channel with the corresponding governor state/control.
     */
    void init();

    /**
     * Thing handler calls this method in order to update the governor with the provided command/state.
     * The provided channel UID must be used to decide if the command belongs to the corresponding governor.
     * @param channelUID channel UID
     * @param command a command or a new state
     */
    void handleCommand(ChannelUID channelUID, Command command);

    /**
     * Thing handler calls this method when it gets disposed so that some resources can be released
     * (e.g. governor destroyed).
     */
    void dispose();

}
