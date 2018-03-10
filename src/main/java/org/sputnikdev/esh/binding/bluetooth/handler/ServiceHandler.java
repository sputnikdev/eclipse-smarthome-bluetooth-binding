package org.sputnikdev.esh.binding.bluetooth.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothSmartDeviceListener;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattService;

import java.util.List;
import java.util.Map;

/**
 * A multi-channel bluetooth device handler which represents a parsable advertising service data
 * where each channel is mapped to corresponding GATT field of the advertised service data
 * (a characteristic effectively).
 *
 * @author Vlad Kolotov
 */
class ServiceHandler extends GattChannelHandler implements BluetoothSmartDeviceListener {

    ServiceHandler(BluetoothHandler handler, URL serviceURL) {
        super(handler, serviceURL.copyWithCharacteristic(serviceURL.getServiceUUID()), true);
    }

    @Override
    public void attach() {
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.addBluetoothSmartDeviceListener(this);
    }

    @Override
    public void detach() {
        DeviceGovernor deviceGovernor = getGovernor();
        deviceGovernor.removeBluetoothSmartDeviceListener(this);
    }

    @Override
    public void servicesResolved(List<GattService> gattServices) { }

    @Override
    public void serviceDataChanged(Map<URL, byte[]> serviceData) {
        if (serviceData.containsKey(url.getServiceURL())) {
            byte[] data = serviceData.get(url.getServiceURL());
            dataChanged(data, true);
        }
    }

    private DeviceGovernor getGovernor() {
        return handler.getBluetoothContext().getManager().getDeviceGovernor(url);
    }

}
