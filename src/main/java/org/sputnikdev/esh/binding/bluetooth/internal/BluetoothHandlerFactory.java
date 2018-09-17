package org.sputnikdev.esh.binding.bluetooth.internal;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;
import org.sputnikdev.bluetooth.manager.transport.BluetoothObjectFactory;
import org.sputnikdev.esh.binding.bluetooth.BluetoothBindingConstants;
import org.sputnikdev.esh.binding.bluetooth.discovery.BluetoothDiscoveryServiceImpl;
import org.sputnikdev.esh.binding.bluetooth.handler.AdapterHandler;
import org.sputnikdev.esh.binding.bluetooth.handler.BeaconBluetoothDeviceHandler;
import org.sputnikdev.esh.binding.bluetooth.handler.BluetoothDeviceHandler;
import org.sputnikdev.esh.binding.bluetooth.handler.GenericBluetoothDeviceHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


/**
 * The {@link BluetoothHandlerFactory} is responsible for creating things and thing handlers.
 * 
 * @author Vlad Kolotov - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, name = "binding.bluetooth")
public class BluetoothHandlerFactory extends BaseThingHandlerFactory {

    private Logger logger = LoggerFactory.getLogger(BluetoothHandlerFactory.class);

    private ServiceRegistration<BluetoothManager> bluetoothManagerServiceRegistration;
    private ServiceRegistration<BluetoothGattParser> gattParserServiceRegistration;
    private BluetoothContext bluetoothContext;
    private ConfigurationAdmin configurationAdmin;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return BluetoothBindingConstants.SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        BluetoothBindingConfig config = getConfig(componentContext);
        updateDiscoveryServiceProperties(config);
        bluetoothContext = new BluetoothContext(getBluetoothManager(config), getGattParser(config), config);
        registerBluetoothObjectFactories();
        publishServices();
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        bluetoothManagerServiceRegistration.unregister();
        bluetoothManagerServiceRegistration = null;
        gattParserServiceRegistration.unregister();
        gattParserServiceRegistration = null;
        bluetoothContext.getManager().dispose();
        bluetoothContext = null;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_ADAPTER)) {
            return new AdapterHandler(thing, bluetoothContext);
        }

        if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_GENERIC)) {
            return new GenericBluetoothDeviceHandler(thing, bluetoothContext);
        }

        if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_BLE)) {
            return new BluetoothDeviceHandler(thing, bluetoothContext);
        }

        if (thingTypeUID.equals(BluetoothBindingConstants.THING_TYPE_BEACON)) {
            return new BeaconBluetoothDeviceHandler(thing, bluetoothContext);
        }

        return null;
    }

    @Reference(unbind = "unregisterBluetoothObjectFactory", cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void registerBluetoothObjectFactory(BluetoothObjectFactory bluetoothObjectFactory) {
        if (bluetoothContext != null) {
            bluetoothObjectFactory.configure(convert(bluetoothContext.getConfig()));
            bluetoothContext.getManager().registerFactory(bluetoothObjectFactory);
        }
    }

    protected void unregisterBluetoothObjectFactory(BluetoothObjectFactory bluetoothObjectFactory) {
        if (bluetoothContext != null) {
            bluetoothContext.getManager().unregisterFactory(bluetoothObjectFactory);
        }
    }

    @Reference(unbind = "unregisterConfigurationAdmin", cardinality = ReferenceCardinality.MANDATORY)
    protected void registerConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unregisterConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }

    private static BluetoothBindingConfig getConfig(ComponentContext componentContext) {
        Map<String, Object> properties = convertDictionary(componentContext.getProperties());
        return parseConfig(properties);
    }


    private static BluetoothManager getBluetoothManager(BluetoothBindingConfig config) {
        return new BluetoothManagerBuilder()
                .withRefreshRate(config.getUpdateRate())
                .withCombinedDevices(config.isCombinedDevicesEnabled())
                .withRediscover(true)
                .withDiscoveryRate(BluetoothDiscoveryServiceImpl.DISCOVERY_RATE_SEC)
                // discovering will be enabled for each adapter individually
                .withDiscovering(false)
                .withStarted(true)
                .build();
    }

    private BluetoothGattParser getGattParser(BluetoothBindingConfig config) {
        BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();
        String extensionFolder = config.getExtensionFolder();
        logger.info("Loading custom GATT specification from folder: {}", extensionFolder);
        File extensionFolderFile = new File(extensionFolder);
        if (extensionFolderFile.exists() && extensionFolderFile.isDirectory()) {
            logger.info("Extension folder exists: {}", extensionFolder);
            gattParser.loadExtensionsFromFolder(config.getExtensionFolder());
        } else {
            logger.warn("Extension folder does not exist, ignoring it: {}", extensionFolder);
        }
        return gattParser;
    }

    private void publishServices() {
        bluetoothManagerServiceRegistration =
                bundleContext.registerService(BluetoothManager.class, bluetoothContext.getManager(), new Hashtable<>());
        gattParserServiceRegistration =
                bundleContext.registerService(BluetoothGattParser.class,
                        bluetoothContext.getParser(), new Hashtable<>());
    }

    private void registerBluetoothObjectFactories() {
        try {
            bundleContext.getServiceReferences(BluetoothObjectFactory.class,null)
                    .forEach(ref -> registerBluetoothObjectFactory(bundleContext.getService(ref)));
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
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

    private void updateDiscoveryServiceProperties(BluetoothBindingConfig config) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration("discovery.bluetooth", null);
            Dictionary<String, Object> props = configuration.getProperties();
            if (props == null) {
                props = new Hashtable<>();
            }
            props.put("background", config.isBackgroundDiscovery());
            configuration.update(props);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

