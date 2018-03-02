# Extending the Bluetooth Binding with custom GATT specifications

## Bluetooth GATT (Generic Attributes)

The Bluetooth GATT define an interaction protocol for bluetooth devices in a declarative manner.
The GATT protocol is described in plain XML files (specifications), more info on that [here](https://www.bluetooth.com/specifications/gatt/generic-attributes-overview)). 
There is an official set of GATT specifications available [here](https://www.bluetooth.com/specifications/gatt). 

## Gatt Parser

The binding leverages [GattParser](https://github.com/sputnikdev/bluetooth-gatt-parser) library in order to handle/consume bluetooth 
raw data and represent it in a user friendly way. The GattParser encapsulates the GATT protocol logic and all available 
official specifications (GATT XML files). The official GATT specifications provide a great variety of generic data structures such as date, time, weigh etc.
which is in most cases more than enough to decode devices state and events. However, some bluetooth device manufacturers use their own
proprietary/custom data structures in their smart devices. The reason might be that their devices are too complex to be described 
with the available data structures, or the manufacturers just want to hide some specifics of their devices.

The binding provides a way to extend the official set of GATT specifications with custom specifications.

## Extending the binding

1. Create your own GATT xml files that describe your custom device. A good example for this could be the Xiaomi MiFlora sensor:
    * Service definition: [com.xiaomi.bluetooth.service.miflora.xml](https://github.com/sputnikdev/bluetooth-gatt-parser/blob/master/src/main/resources/gatt/service/com.xiaomi.bluetooth.service.miflora.xml)
    * Characteristics definitions:
        * [com.xiaomi.bluetooth.characteristic.miflora.xml](https://github.com/sputnikdev/bluetooth-gatt-parser/blob/master/src/main/resources/gatt/characteristic/com.xiaomi.bluetooth.characteristic.miflora.xml)
        * [com.xiaomi.bluetooth.characteristic.battery_firmware.xml](https://github.com/sputnikdev/bluetooth-gatt-parser/blob/master/src/main/resources/gatt/characteristic/com.xiaomi.bluetooth.characteristic.battery_firmware.xml)
        * [com.xiaomi.bluetooth.characteristic.advertised_data.xml](https://github.com/sputnikdev/bluetooth-gatt-parser/blob/master/src/main/resources/gatt/characteristic/com.xiaomi.bluetooth.characteristic.advertised_data.xml)
        
2. Create a root folder on your file system that is accessible by "openhab" user, e.g.:
`/home/pi/.bluetooth_smart`. The root folder should contain two sub-folders:
   * "characteristic"
   * "service"    
3. These two folders should contain your custom GATT xml files respectively, e.g. the "characteristic" folder contains XML files 
for characteristics and the "service" contains XML files for your services. **Note: Make sure that permissons for files and folders are set correctly so that "openhab" user can access them.**
4. Configure the binding with the root folder here:

![Configure GATT extensions](gatt-extensions.png?raw=true "Configure GATT extensions")
5. Once everything is configured properly, the binding creates OH "thing" channels for your device automatically in accordance with your custom GATT specifications:

![Xiaomi MiFlora thing](xiaomi-miflora-thing.png?raw=true "Xiaomi MiFlora thing")