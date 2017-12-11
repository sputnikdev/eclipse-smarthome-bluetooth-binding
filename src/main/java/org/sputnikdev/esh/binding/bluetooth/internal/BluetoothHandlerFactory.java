package org.sputnikdev.esh.binding.bluetooth.internal;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerFactory;
import org.sputnikdev.bluetooth.manager.impl.BluetoothObjectFactoryProvider;
import org.sputnikdev.bluetooth.manager.transport.BluetoothObjectFactory;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.handler.AdapterHandler;
import org.sputnikdev.esh.binding.bluetooth.handler.BluetoothDeviceHandler;
import org.sputnikdev.esh.binding.bluetooth.handler.GenericBluetoothDeviceHandler;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The {@link BluetoothHandlerFactory} is responsible for creating things and thing
 * handlers.
 * 
 * @author Vlad Kolotov - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, name = "binding.bluetooth",
        configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class BluetoothHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS =
        Stream.of(BluetoothBindingConstants.THING_TYPE_ADAPTER, BluetoothBindingConstants.THING_TYPE_GENERIC,
            BluetoothBindingConstants.THING_TYPE_BLE).collect(Collectors.toSet());

    private ServiceRegistration<BluetoothManager> bluetoothManagerServiceRegistration;
    private BluetoothManager bluetoothManager;
    private ServiceRegistration<BluetoothGattParser> bluetoothGattParserServiceRegistration;
    private BluetoothGattParser gattParser;
    private ItemRegistry itemRegistry;

    private int initialOnlineTimeout = BluetoothBindingConstants.INITIAL_ONLINE_TIMEOUT;
    private boolean initialConnectionControl = BluetoothBindingConstants.INITIAL_CONNECTION_CONTROL;
    private Map<String, Object> config;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        Dictionary<String, Object> properties = componentContext.getProperties();
        String extensionFolder = (String) properties.get(BluetoothBindingConstants.BINDING_CONFIG_EXTENSION_FOLDER);
        String refreshRate = (String) properties.get(BluetoothBindingConstants.BINDING_CONFIG_UPDATE_RATE);
        Boolean expertMode = (Boolean) properties.get(BluetoothBindingConstants.BINDING_CONFIG_EXPERT_MODE);
        initialOnlineTimeout = NumberUtils.toInt(
                (String) properties.get(BluetoothBindingConstants.BINDING_CONFIG_INITIAL_ONLINE_TIMEOUT),
                BluetoothBindingConstants.INITIAL_ONLINE_TIMEOUT);
        initialConnectionControl = BooleanUtils.toBooleanDefaultIfNull((Boolean) properties.get(
                BluetoothBindingConstants.BINDING_CONFIG_INITIAL_CONNECTION_CONTROL),
                BluetoothBindingConstants.INITIAL_CONNECTION_CONTROL);

        Iterator<String> keysIter = Iterators.forEnumeration(properties.keys());
        config = Maps.toMap(keysIter, properties::get);

        BluetoothObjectFactoryProvider.getRegisteredFactories().forEach(factory -> {
            factory.configure(config);
        });

        if (StringUtils.isBlank(extensionFolder)) {
            extensionFolder = "/home/pi/.bluetooth_smart";
        }
        bluetoothManager = BluetoothManagerFactory.getManager();
        if (refreshRate != null && NumberUtils.isNumber(refreshRate)) {
            bluetoothManager.setRefreshRate(NumberUtils.toInt(refreshRate));
        }
        bluetoothManager.setSharedMode(!BooleanUtils.toBoolean(expertMode));
        gattParser = BluetoothGattParserFactory.getDefault();
        File extensionFolderFile = new File(extensionFolder);
        if (extensionFolderFile.exists() && extensionFolderFile.isDirectory()) {
            gattParser.loadExtensionsFromFolder(extensionFolder);
        }
        bluetoothManagerServiceRegistration = (ServiceRegistration<BluetoothManager>)
            bundleContext.registerService(BluetoothManager.class.getName(), bluetoothManager, new Hashtable<>());
        bluetoothGattParserServiceRegistration = (ServiceRegistration<BluetoothGattParser>)
            bundleContext.registerService(BluetoothGattParser.class.getName(), gattParser, new Hashtable<>());
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        bluetoothManagerServiceRegistration.unregister();
        bluetoothGattParserServiceRegistration.unregister();
        BluetoothManagerFactory.dispose(bluetoothManager);
        bluetoothManager = null;
        gattParser = null;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_ADAPTER)) {
            return new AdapterHandler(thing, itemRegistry, bluetoothManager, gattParser);
        } else if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_GENERIC)) {
            GenericBluetoothDeviceHandler genericBluetoothDeviceHandler =
                    new GenericBluetoothDeviceHandler(thing, itemRegistry, bluetoothManager, gattParser);
            genericBluetoothDeviceHandler.setInitialOnlineTimeout(initialOnlineTimeout);
            return genericBluetoothDeviceHandler;
        } else if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_BLE)) {
            BluetoothDeviceHandler bluetoothDeviceHandler =
                    new BluetoothDeviceHandler(thing, itemRegistry, bluetoothManager, gattParser);
            bluetoothDeviceHandler.setInitialOnlineTimeout(initialOnlineTimeout);
            bluetoothDeviceHandler.setInitialConnectionControl(initialConnectionControl);
            return bluetoothDeviceHandler;
        }

        return null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Reference(unbind = "unregisterBluetoothObjectFactory", cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    public void registerBluetoothObjectFactory(BluetoothObjectFactory bluetoothObjectFactory) {
        if (config != null) {
            bluetoothObjectFactory.configure(config);
        }
        BluetoothObjectFactoryProvider.registerFactory(bluetoothObjectFactory);
    }

    public void unregisterBluetoothObjectFactory(BluetoothObjectFactory bluetoothObjectFactory) {
        BluetoothObjectFactoryProvider.unregisterFactory(bluetoothObjectFactory);
    }
}

