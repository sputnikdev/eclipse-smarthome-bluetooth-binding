package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.Command;
import org.sputnikdev.bluetooth.URL;

/**
 * @author Vlad Kolotov
 */
public interface ChannelHandler {

    void init();

    void handleCommand(ChannelUID channelUID, Command command);

    void dispose();

    URL getURL();

}
