package org.sputnikdev.esh.binding.bluetooth.handler;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.CharacteristicFormatException;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.spec.Field;
import org.sputnikdev.bluetooth.manager.BluetoothGovernor;
import org.sputnikdev.esh.binding.bluetooth.internal.BluetoothUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

abstract class GattChannelHandler implements ChannelHandler {

    private Logger logger = LoggerFactory.getLogger(GattChannelHandler.class);
    protected final BluetoothHandler handler;
    protected final URL url;
    private boolean linked;
    private final boolean readOnly;
    private final boolean advanced;
    private final boolean recognised;
    protected final boolean binary;

    GattChannelHandler(BluetoothHandler handler, URL url, boolean readOnly) {
        this.handler = handler;
        this.url = url;
        this.readOnly = readOnly;
        advanced = handler.getBindingConfig().getAdvancedGattServices().contains(url.getServiceUUID());
        binary = handler.getBindingConfig().discoverBinaryOnly();
        recognised = handler.getParser().isKnownCharacteristic(url.getCharacteristicUUID());
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void init() { /* do nothing */ }

    @Override
    public final void linked() {
        synchronized (url) {
            if (!linked) {
                attach();
                linked = true;
            }
        }
    }

    @Override
    public final void unlinked() {
        synchronized (url) {
            if (linked) {
                detach();
                linked = false;
            }
        }
    }

    @Override
    public void dispose() {
        detach();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GattChannelHandler)) {
            return false;
        }
        GattChannelHandler that = (GattChannelHandler) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    protected abstract void attach();

    protected abstract void detach();

    protected List<Channel> buildChannels() {
        if (binary || !recognised) {
            return Collections.singletonList(buildBinaryChannel());
        } else {
            return buildFieldChannels();
        }
    }

    protected void dataChanged(byte[] data, boolean buildMissingChannels) {
        if (binary || !recognised) {
            if (buildMissingChannels) {
                buildMissingBinaryChannel();
            }
            updateBinaryState(data);
        } else {
            Collection<FieldHolder> holders = parseData(data);
            if (buildMissingChannels) {
                buildMissingChannels(holders.stream().map(FieldHolder::getField).collect(Collectors.toList()));
            }
            holders.forEach(holder -> {
                Channel channel = getChannel(holder.getField());
                if (channel != null) {
                    updateState(channel, holder);
                }
            });
        }
    }

    private List<Channel> buildFieldChannels() {
        List<Field> fields = handler.getParser().getFields(url.getCharacteristicUUID())
                .stream().filter(this::channelRequired).collect(Collectors.toList());
        BluetoothChannelBuilder builder = new BluetoothChannelBuilder(handler);
        logger.debug("Building channels for fields: {} / {}", url, fields.size());
        return builder.buildChannels(url, fields, advanced, readOnly);
    }

    private Channel buildBinaryChannel() {
        logger.debug("Building a new binary channel for service data url: {}", url);
        BluetoothChannelBuilder builder = new BluetoothChannelBuilder(handler);
        return builder.buildBinaryChannel(url, advanced, readOnly);
    }

    private Collection<FieldHolder> parseData(byte[] value) {
        if (value.length == 0) {
            logger.warn("Attribute value is empty: {}", url);
            return Collections.emptyList();
        }
        try {
            return handler.getParser().parse(url.getCharacteristicUUID(), value).getFieldHolders();
        } catch (CharacteristicFormatException ex) {
            logger.error("Could not parse data: {}. Ignoring it...", url, ex);
            return Collections.emptyList();
        }
    }

    private void buildMissingChannels(Collection<Field> fields) {
        Map<Boolean, List<Field>> partitioned = fields.stream()
                .filter(this::channelRequired)
                .collect(Collectors.partitioningBy(field -> getChannel(field) == null));

        if (!partitioned.get(Boolean.TRUE).isEmpty()) {
            BluetoothChannelBuilder builder = new BluetoothChannelBuilder(handler);
            logger.debug("Building missing channels for fields: {} / {}", url, fields.size());
            List<Channel> channels = builder.buildChannels(url, partitioned.get(Boolean.TRUE), advanced, readOnly);
            channels.forEach(channel -> handler.registerChannel(channel.getUID(), this));
            handler.updateThingWithChannels(channels);
        }

        partitioned.get(Boolean.FALSE).forEach(field -> {
            handler.registerChannel(getChannel(field).getUID(), this);
        });
    }

    private boolean channelRequired(Field field) {
        return !field.isFlagField() && !field.isOpCodesField()
                && (!field.isUnknown() && !field.isSystem() || handler.getBindingConfig().discoverUnknown());
    }

    private void buildMissingBinaryChannel() {
        if (getBinaryChannel() == null) {
            Channel channel = buildBinaryChannel();
            handler.registerChannel(channel.getUID(), this);
            handler.updateThingWithChannels(Collections.singletonList(channel));
        }
    }

    private void updateState(Channel channel, FieldHolder holder) {
        handler.updateState(channel.getUID(), BluetoothUtils.convert(handler.getParser(), holder));
    }

    private void updateBinaryState(byte[] data) {
        Channel channel = getBinaryChannel();
        if (channel != null) {
            handler.updateState(channel.getUID().getIdWithoutGroup(),
                    new StringType(handler.getBluetoothContext().getParser().parse(data, 16)));
        } else {
            logger.error("Could not find binary channel: {}", url);
        }
    }

    private Channel getChannel(Field field) {
        return handler.getThing().getChannel(BluetoothUtils.getChannelUID(url.copyWithField(field.getName())));
    }

    private Channel getBinaryChannel() {
        return handler.getThing().getChannel(BluetoothUtils.getChannelUID(url));
    }

    private BluetoothGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getGovernor(url);
    }
}
