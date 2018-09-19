
# Advanced Bluetooth Binding

The Advanced Bluetooth binding brings support for Bluetooth technology into Eclipse SmartHome Framework.
The following main use cases are supported by the binding:

* **Presence detection**. Bluetooth devices can be monitored whether they are in range of a bluetooth adapter (or multiple bluetooth adapters). 
This feature makes it possible to detect if an object or a person enters or leaves a building or a room.
* **Indoor positioning**. Location of a Bluetooth device can be identified based on estimated distance between the device and adapters.
This feature allows you to locate things (like your phone or keys) in your house or detect who exactly is in a room.  
* **Comprehensive support for Bluetooth Low Energy (Bluetooth Smart) devices**. If a bluetooth device supports the Bluetooth specification 4.0, 
then the standard [GATT services and characteristics](https://www.bluetooth.com/specifications/gatt) can be automatically recognized and 
corresponding thing channels be created for each GATT characteristic and its GATT fields. This feature allows you to connect 
Bluetooth Smart sensors and other Bluetooth devices.
* **Custom built (non-standard) BLE bluetooth devices support**. A custom built Bluetooth device can be automatically recognized like a standard one
by specifying a folder on the system disk with custom GATT service and characteristic definitions in XML files.

Table of contents
=================

<!--ts-->
   * [Installation](#installation)
   * [Supported Things and Discovery](#supported-things-and-discovery)
      * [Bluetooth Adapter](#bluetooth-adapter)
      * [Generic Bluetooth device](#generic-bluetooth-device)
      * [BLE enabled Bluetooth device](#ble-enabled-bluetooth-device)
      * [Beacon Bluetooth device](#beacon-bluetooth-device)
   * [Installation](#installation)
   * [Usage](#usage)
      * [Presence detection](#presence-detection)
      * [Indoor positioning](#indoor-positioning)
      * [Bluetooth Smart (BLE) devices](#bluetooth-smart-(ble)-devices)
      * [Extending the binding](#extending-the-binding)
   * [Bluetooth adapters compatibility matrix](#bluetooth-adapters-compatibility-matrix)
   * [Implementation notes](#implementation-notes)
   * [Troubleshooting](#troubleshooting)
   * [Roadmap](#roadmap)
<!--te-->


## Installation

The Bluetooth binding can be installed via PaperUI and Eclipse MarketPlace. Please follow this [guide](installation.md).

## Supported Things and Discovery

### Bluetooth Adapter 
There is only one "thing" that represents all adapters (Generic and BlueGiga) in the binding.
* Generic adapters should appear in the discovery inbox without making any configuration changes:
![ESH Bluetooth Binding adaptes discivery](binding-tinyb-adapters-discovery.png?raw=true "ESH Bluetooth Binding adapters discovery") 
* BlueGiga adapters will appear after configuring serial port regular expression on the binding configuration page:
![ESH Bluegiga adapters configure](binding-bluegiga-adapter-configure.png?raw=true "ESH Bluegiga adapters configure")
**WARNING!:** It is very important to make the regular expression **NOT** too wide so that ONLY Bluegiga adapters/ serial ports are matched by the regular expression. 
If the regular expression is too broad, this can lead to hardware malfunction/damage of other devices that are accidentally matched by the regular expression. **USE THIS FEATURE AT YOUR OWN RISK**.
Regular expression examples:
    - Defining some specific serial ports (preferable option): (/dev/ttyACM1)|(/dev/ttyACM2)
    - Matching all serial ports on Linux: ((/dev/ttyACM)[0-9]{1,3})
    - Matching all serial ports on OSX: (/dev/tty.(usbmodem).*)
    - Matching all serial ports on Windows: ((COM)[0-9]{1,3})
    - Matching all serial ports with some exclusions: (?!/dev/ttyACM0|/dev/ttyACM5)((/dev/ttyACM)[0-9]{1,3})
    - Default regular expression is to match nothing: (?!)

Discovered adapters can be added as new things from the PaperUI inbox. It is possible to enable/disable discovery of Bluetooth devices for each added adapter thing:
![ESH Bluetooth adapter](binding-adapter.png?raw=true "ESH Bluetooth adapter")

### Generic Bluetooth device
This thing is intended to be used for older versions of bluetooth devices which do not support BLE technology. 

Some of mobile phones (older versions) are recognised as generic bluetooth device:
![ESH Bluetooth generic device discovery](binding-generic-device-discovery.png?raw=true "ESH Bluetooth generic device discovery")
 
Only Presence detection and Indoor positioning features are available for this type of Bluetooth devices:
![ESH Bluetooth generic device](binding-generic-device.png?raw=true "ESH Bluetooth generic device")

### BLE enabled Bluetooth device
This thing is used for newer versions of Bluetooth devices which support BLE technology (public and private static address types).
![ESH Bluetooth ble device discovery](binding-ble-device-discovery.png?raw=true "ESH Bluetooth ble device discovery")

All the binding features are fully supported by this thing. For example, a standard Bluetooth heart rate monitor will look like that:
![ESH Bluetooth ble device](binding-ble-device.png?raw=true "ESH Bluetooth ble device")

From the picture above you can see a list of channels that are automatically created for all discovered GATT services and characteristics of the heart rate monitor device.

### Beacon Bluetooth device

This thing is used for some Bluetooth devices that periodically change its addresses (private non-resolvable and resolvable address types) to some random addresses in order to protect privacy of their owners.
All modern mobile phones (iPhone and Android) evey so often (roughly every 10 minutes) randomly generate a new Bluetooth address and abandon old one making it impossible to track them.

Only Presence detection and Indoor positioning features are available for this type of Bluetooth devices:
![ESH Beacon Bluetooth device](binding-beacon-device.png?raw=true "ESH Beacon Bluetooth device")

## Usage

### Presence detection

Bluetooth devices are able to broadcast (advertise) some information that can be used to identify them via radio signal, normally it is:
* Device name
* Device address
* Service identifiers and data
* Manufacturer data

Bluetooth receivers (adapters) periodically scan radio frequencies for advertised messages and report them to the binding.
By using advertised messages the binding is able automatically discover Bluetooth devices and also track its presence.

The easiest and most effective way to detect presence is to use so called ["Bluetooth beacon"](tags.md) devices.

Normally, most of Bluetooth devices broadcast messages, however there are some devices that try to hide themselves from receivers providing privacy for their owners.
Many of modern mobile phones (iPhone and Android) evey so often (roughly every 10 minutes) randomly generate a new Bluetooth address and abandon old one making it impossible to track them by its address.

The binding implements an algorithm to match Bluetooth devices not by its address but by a combination of the following:
* Advertised device name
* Advertised service UUID/value (not yet implemented)

Moreover, Android and iPhone smartphones also stop advertising (turns off broadcasting so that no any Bluetooth device can find them) once Bluetooth settings screen gets closed.
Some mobile apps can be used to enforce Bluetooth broadcasting to be always on, they normally called "Bluetooth beacon simulators".

//TODO list some beacon simulators

### Indoor positioning

Bluetooth adapters are capable of sensing strength of radio signal that is transmitted by Bluetooth devices (Received signal strength indication; RSSI).
The binding is able to calculate estimated distance between adapters and devices by analysing RSSI levels. The closer device to adapter, the stronger signal.
If multiple adapters are installed into the system, the binding can be used to detect location of Bluetooth device by comparing estimated distance between device and adapters.

Multiple adapters must be used in order the indoor positioning to functional effectively.

It is advisable that adapters are physically separated from each other as much as possible.
For example, you may want to install Bluetooth adapters in each room of your house so that the binding will be able to locate Bluetooth devices in each room.
Therefore defining a location for each adapter is essential:
![ESH Bluetooth adapter location](binding-adapter-location.png?raw=true "ESH Bluetooth adapter location")

The location of the nearest adapter will be reported by Bluetooth device thing:
![ESH Bluetooth generic device](binding-generic-device.png?raw=true "ESH Bluetooth generic device")

### Bluetooth Smart (BLE) device

* [Xiaomi](xiaomi.md)
* Smart thermostats (todo)
* Some blinds (todo)

## Extending the binding

In order to add support for new devices, the binding can be extended in several ways - declarative and programmatic:

* [Declarative](gatt-extensions.md)
* //TODO programmatic

## Implementation notes

The binding extensively uses [Java Bluetooth Manager](https://github.com/sputnikdev/bluetooth-manager) and [Bluetooth GATT Parser](https://github.com/sputnikdev/bluetooth-gatt-parser).
More details can be found [here](implementation-notes.md).

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

* [BlueGiga adapters are not getting discovered](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/6)
* [Generic adapters (TinyB transport) are not getting discovered](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/7)
* [Battery service does not get resolved](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/8)
* [I've added an adapter, but I can't see any bluetooth devices](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/11)
* Nothing happens and I have errors in the log file:<br/>
    - [GDBus.Error:org.freedesktop.DBus.Error.AccessDenied](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/9)
    - [org.sputnikdev.bluetooth.manager.NotReadyException: Could not power adapter](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/10) 
    
## Roadmap

1. [~~Clean up Bluetooth Manager API so it can be shared between multiple clients~~](https://github.com/sputnikdev/bluetooth-manager/issues/5).
2. Test and add support for the following devices:
    - [~~Xiaomi Mijia Bluetooth Temperature Smart Humidity Sensor~~](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/18)
    - [~~Xiaomi Mi Flora Monitor~~](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/19)
    - ~~iTag Bluetooth tracking tag (beacon)~~
    - [~~Elistooop Bluetooth tracking tag (beacon)~~](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/20)
    - [~~Dehyaton Bluetooth tracking tag (beacon)~~](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/21)
    - [Smart Radiator Thermostat - eQ-3](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/13)
    - [Smart Radiator Thermostat - comet-blue-eurotronic](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/14)
    - [Oregon RAR213HG weather station](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/27)
    - [Playbulb candle](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/37)
    - [Yeelight bulb](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/38)
    - [HRAEFN Bluetooth tracking tag (beacon)](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/22)
3. [Implement a workaround for mobile phones (recent models of iphone and android) that dynamically allocate MAC addresses](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/17) so the such phones can be used for presence detection.
4. [Improve indoor positioning performance and accuracy](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/23).
5. [Create a new template project (bluetooth binding extension) which demonstrates how to create custom bluetooth bindings](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/16).
6. **The ultimate goal:** provide a comprehensive and integrated environment for building custom bluetooth devices (sensors) based on the Bluetooth binding, cheap $2 bluetooth board (nrf51822) and [Mbed OS](https://www.mbed.com/en/platform/mbed-os/). This should bring an easy way to build your own wireless sensors for a very affordable price.  

