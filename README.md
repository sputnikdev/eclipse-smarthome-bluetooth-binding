
# Bluetooth Binding

The Bluetooth binding brings support for bluetooth devices into Eclipse SmartHome Framework. 
The following main use cases are supported by the binding:

* Presence detection. Bluetooth devices can be monitored whether they are in range of a Bluetooth adapter (or multiple Bluetooth adapters). 
This feature makes it possible to detect if an object or a person enters or leaves a building or a room.
* Indoor positioning. Location of a Bluetooth device can be identified based on estimated distance between the device and adapters. 
This feature allows you to locate things (like your phone or keys) in your house or detect who exactly is in a room.  
* Comprehensive support for Bluetooth Low Energy (Bluetooth Smart) devices. If a bluetooth device supports Bluetooth specification 4.0, 
then the standard [GATT services and characteristics](https://www.bluetooth.com/specifications/gatt) can be automatically recognized and 
corresponding thing channels be created for each GATT characteristic and its GATT fields. This feature allows you to connect 
Bluetooth smart sensors and other Bluetooth devices.
* Custom built (non-standard) BLE Bluetooth devices support. A custom built Bluetooth device can be automatically recognized like a standard one 
by specifying a folder on the system disk with custom GATT service and characteristic definitions in XML files.

## Installation

The Bluetooth binding can be installed via PaperUI and Eclipse MarketPlace. 

1. The Eclipse MarketPlace add-on should be installed first (it might be a bit confusing but the MarketPlace add-on is listed as "Eclipse IoT Market"):
![Eclipse MarketPlace installation](eclipse-iot-install.png?raw=true "Eclipse MarketPlace installation")
2. Once the MarketPlace add-on is installed it should be configured to display "betta" and "alpha" listings:
![Eclipse MarketPlace configuration](eclipse-iot-configure.png?raw=true "Eclipse MarketPlace configuration")
3. You may also want to enable the "Item Linking - Simple Mode" to automatically create thing items:
![ESH Enabling Simple Linking](esh-simple-linking.png?raw=true "ESH Enabling Simple Linking")
4. Depending on what Bluetooth adapter you have you will need to install one of the bluetooth transport extensions
(or all of them if you have both types of bluetooth adapters). See [the compatibility matrix](#bluetooth-adapters-compatibility-matrix) below.
![Eclipse MarketPlace Bluetooth Transport installation](eclipse-iot-bluetooth-transport.png?raw=true "Eclipse MarketPlace Bluetooth Transport installation")
Note: There are some certain prerequisites that must be met for the generic adapter type (TinyB Bluetooth Transport) to work properly. 
Please follow [these steps](https://github.com/sputnikdev/bluetooth-manager-tinyb#prerequisites) to find out if you need to do anything special.
5. Finally install the Bluetooth Binding:
![ESH Bluetooth Binding installation](eclipse-iot-bluetooth-binding.png?raw=true "ESH Bluetooth Binding installation")

## Supported Things and Discovery

### Bluetooth Adapter 
There is only one "thing" that represents all adapters (Generic and BlueGiga) in the Binding.
* Generic adapters should appear in the discovery inbox without making any configuration changes:
![ESH Bluetooth Binding adaptes discivery](binding-tinyb-adapters-discovery.png?raw=true "ESH Bluetooth Binding adapters discovery") 
* BlueGiga adapters will appear after configuring serial ports regular expression on the binding configuration page:
![ESH Bluegiga adapters configure](binding-bluegiga-adapter-configure.png?raw=true "ESH Bluegiga adapters configure")
WARNING!: It is very important not to make the regular expression too wide so that ONLY Bluegiga adapters/ serial ports are matched by the regular expression. 
If the regular expression is too broad, this can lead to hardware malfunction/damage of other devices that are accidentally matched by the regular expression. USE THIS FEATURE AT YOUR OWN RISK.<br/>
Regular expression examples:<br/>
    - Defining some specific serial ports (preferable option): (/dev/ttyACM1)|(/dev/ttyACM2)
    - Matching all serial ports on Linux: ((/dev/ttyACM)[0-9]{1,3})
    - Matching all serial ports on OSX: (/dev/tty.(usbmodem).*)
    - Matching all serial ports on Windows: ((COM)[0-9]{1,3})
    - Matching all serial ports with some exclusions: (?!/dev/ttyACM0|/dev/ttyACM5)((/dev/ttyACM)[0-9]{1,3})
    - Default regular expression is to match nothing: (?!)
### Generic Bluetooth device
This thing is intended to be used for older versions of Bluetooth devices which do not support BLE technology. 

Most of the mobile phones are recognised as generic bluetooth devices:
![ESH Bluetooth generic device discovery](binding-generic-device-discovery.png?raw=true "ESH Bluetooth generic device discovery")
 
Only Presence detection and Indoor positioning features are available for this type of Bluetooth devices:
![ESH Bluetooth generic device](binding-generic-device.png?raw=true "ESH Bluetooth generic device")

### BLE enabled Bluetooth device
This thing is used for newer versions of Bluetooth devices which support BLE technology. All binding features are supported by this thing. 

## Presence detection



## Indoor positioning

## Bluetooth Smart (BLE) devices

## Bluetooth adapters compatibility matrix

There are two types of adapters that are supported by the binding. Each of them has pros and cons. You may find that 
not all of them will work for you:
* Generic adapter (Supported by the TinyB Transport SmartHome extension). This a type of bluetooth adapters that you can buy from your local computer store. 
These are the most common adapters, most likely you have one already or it is embedded into your PC/laptop (or Raspberry PI). 
However, there is a major drawback - it works only in Linux based environments, to be more precise, it requires Bluez software installed in your OS. 
* BlueGiga serial adapter (Supported by the BlueGiga Transport SmartHome extension). This a special type of bluetooth adapters that is quite rare. It is not easy to find it in a local computer store, 
but still possible to buy it over the Internet. One of the most common is [Bluegiga BLED112](https://www.silabs.com/products/wireless/bluetooth/bluetooth-low-energy-modules/bled112-bluetooth-smart-dongle).
The main criteria in selecting a BlueGiga adapter is that it must support [BGAPI](https://www.silabs.com/community/wireless/bluetooth/knowledge-base.entry.html/2015/08/06/_reference_bgapib-Alq6) (check this before you buy an adapter).

The following table shows some major features supported by each adapter type: 

|                                     |    Generic    |   BlueGiga    | 
|     :---                            |     :---:     |     :---:     |
| Windows                             |       -       |       Y       |
| Linux                               |       Y       |       Y       |
| Mac                                 |       -       |       Y       |
| X86 32bit                           |       Y       |       Y       |
| X86 64bit                           |       Y       |       Y       |
| ARM v6 (Raspberry PI)               |       Y       |       Y       |
| Adapters autodiscovery              |       Y       |       Y       |
| Adapter aliases                     |       Y       |       -       |
| Device aliases                      |       Y       |       -       |
| BLE devices support                 |       Y       |       Y       |
| BR/EDR devices support (legacy BT)  |       Y       |       -       |

## Troubleshooting

* BlueGiga adapters are not getting discovered:
  - Make sure that Eclipse SmartHome (opehab system user) has sufficient permissions to access adapters (serial ports) in your OS (e.g. `sudo adduser openhab dialout`)
  - Double check if the regular expression to look up serial ports is valid and correctly selects BlueGiga serial ports (and ony BlueGiga serial ports).
* Generic adapters (TinyB transport) are not getting discovered:
  - Make sure that Bluez software is of a recent version (v5.43+). Check [here]((https://github.com/sputnikdev/bluetooth-manager-tinyb#prerequisites)).