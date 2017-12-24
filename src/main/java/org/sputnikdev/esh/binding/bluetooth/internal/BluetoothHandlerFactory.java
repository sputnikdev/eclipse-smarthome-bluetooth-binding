package org.sputnikdev.esh.binding.bluetooth.internal;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
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
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;
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
    private BluetoothBindingConfig config;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);

        Map<String, Object> properties = convertDictionary(componentContext.getProperties());
        config = parseConfig(properties);

        BluetoothObjectFactoryProvider.getRegisteredFactories().forEach(factory -> {
            factory.configure(properties);
        });
        bluetoothManager = BluetoothManagerFactory.getManager();
        bluetoothManager.setRefreshRate(config.getUpdateRate());
        bluetoothManager.setSharedMode(!config.isAdvancedMode());
        gattParser = BluetoothGattParserFactory.getDefault();
        File extensionFolderFile = new File(config.getExtensionFolder());
        if (extensionFolderFile.exists() && extensionFolderFile.isDirectory()) {
            gattParser.loadExtensionsFromFolder(config.getExtensionFolder());
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
            genericBluetoothDeviceHandler.setInitialOnlineTimeout(config.getInitialOnlineTimeout());
            return genericBluetoothDeviceHandler;
        } else if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_BLE)) {
            BluetoothDeviceHandler bluetoothDeviceHandler =
                    new BluetoothDeviceHandler(thing, itemRegistry, bluetoothManager, gattParser);
            bluetoothDeviceHandler.setInitialOnlineTimeout(config.getInitialOnlineTimeout());
            bluetoothDeviceHandler.setInitialConnectionControl(config.isInitialConnectionControl());
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
            bluetoothObjectFactory.configure(convert(config));
        }
        BluetoothObjectFactoryProvider.registerFactory(bluetoothObjectFactory);
    }

    public void unregisterBluetoothObjectFactory(BluetoothObjectFactory bluetoothObjectFactory) {
        BluetoothObjectFactoryProvider.unregisterFactory(bluetoothObjectFactory);
    }

    private static BluetoothBindingConfig parseConfig(Map<String, Object> properties) {
        BluetoothBindingConfig config = new BluetoothBindingConfig();
        try {
            BeanUtils.copyProperties(config, properties);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        return config;
    }

    private static Map<String, Object> convertDictionary(Dictionary<String, Object> properties) {
        return Maps.toMap(Iterators.forEnumeration(properties.keys()), properties::get);
    }

    private static Map<String, Object> convert(BluetoothBindingConfig config) {
        try {
            return BeanUtilsBean.getInstance().getPropertyUtils().describe(config);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}

