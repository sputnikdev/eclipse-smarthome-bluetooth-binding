# bluetooth-binding

WIP: Eclipse SmartHome Bluetooth Binding.

This binding is still work in progress. The main goal is to merge [vkolotov](https://github.com/vkolotov)'s [OH BluetoothSmart binding](https://github.com/openhab/openhab2-addons/pull/2489) and [cdjackson](https://github.com/cdjackson)'s [BLE binding](https://github.com/eclipse/smarthome/pull/3633).

## Objectives and KPIs

### Objectives

1. Merge the two PRs
2. Productionize the result

### KPIs

1. Flexibility in using different transports, e.g. serial port, DBus or any other (like tinyb).
2. Extensibility in adding new supported devices (new OH addons), e.g. different sensors and other hardware.
3. Robustness. Due to the nature of the Bluetooth protocol, we will have to make our bindings stable enough so that people could use it. This is the biggest challenge for this project.
4. Comprehensive support for Bluetooth GATT specifications. This is a powerful feature which would allow users:
    - add any device which conforms GATT specification without developing any new binding
    - add custom made devices by only specifying a path to custom defined GATT specification for a device

## Progress
1. ~~Merging BluetoothSmart binding into ESH project~~
2. ~~Splitting BluetoothSmart binding into two bundles: bluetooth and transport (tinyb) bundles~~
3. ~~Making adapter things to be bridges~~
4. Extracting tinyb library into a separate bundle (this should resolve the issue "native library already loaded in another classloader")
5. Implementing/merging BlueGiga transport/bundle - 70% DONE.
6. Implementing a basic binding for a specific device (POC) to discover possible options for future binding implementations. GATT specification driven and custom code/logic driven
7. Merging YeeLightBlue binding/bundle
8. Implementing DBus transport
9. Stabilizing

## Implementation details

### 1. Bluetooth URL
![bluetooth url](https://user-images.githubusercontent.com/1161883/28913226-6259477e-788b-11e7-8765-6e58667a4a6c.png)

[This is](https://github.com/sputnikdev/bluetooth-utils/blob/master/src/main/java/org/sputnikdev/bluetooth/URL.java) a useful component which allows us to identify any BT resource. It is similar to @cdjackson 
 [BluetoothAddress](https://github.com/cdjackson/smarthome/blob/ble_bundle/extensions/binding/org.eclipse.smarthome.binding.ble/src/main/java/org/eclipse/smarthome/binding/ble/BluetoothAddress.java), but it also supports locating BT adapters, services, characteristics and GATT fields. Even if multiple adapters are used, the URL can precisely identify resource of a BT device. That class is reasonably documented, so take a look at the comments in the file for more info.

The Bluetooth URL can help to address Flexibility and Extensibility KPIs.

### 2. Transport abstraction layer
![transport abstraction layer](https://user-images.githubusercontent.com/1161883/28913669-f6be0d7c-788c-11e7-8b01-cdade0abb279.png)

This is an API which is supposed to provide an abstraction layer between hardware and OpenHab. The API provides common interfaces for BT adapter, device, GATT service, GATT characteristic. It also supports plugging in different implementations for different transport types, such us serial port transport or BDus transport. In order to do so, a new transport layer must implement a limited number (actually only 4 interfaces, an example [here](https://github.com/sputnikdev/bluetooth-manager/tree/master/src/main/java/org/sputnikdev/bluetooth/manager/impl/tinyb)) of interfaces.

This abstraction layer can help to address Flexibility and partially Robustness.

As a first step, I would leave TinyB transport as a main transport for linux based set-ups, however I agree that this should be converted to be using native DBus adapter. I can even suggest a good candidate for this work - [hypfvieh](https://github.com/hypfvieh/bluez-dbus). I had a chat with David a couple of months ago, he might be interested in this.

### 3. Bluetooth Manager
![bluetooth manager](https://user-images.githubusercontent.com/1161883/28918587-f23f2fe0-789d-11e7-8d72-b5ca56ab215b.png)

It is a set of APIs and processes which are responsible for robustness of the system. The central part of it is "Governors" (examples and more info [BluetoothGovernor](https://github.com/sputnikdev/bluetooth-manager/blob/master/src/main/java/org/sputnikdev/bluetooth/manager/BluetoothGovernor.java),  [AdapterGovernor](https://github.com/sputnikdev/bluetooth-manager/blob/master/src/main/java/org/sputnikdev/bluetooth/manager/AdapterGovernor.java), [DeviceGovernor](https://github.com/sputnikdev/bluetooth-manager/blob/master/src/main/java/org/sputnikdev/bluetooth/manager/DeviceGovernor.java) and [BluetoothManager](https://github.com/sputnikdev/bluetooth-manager/blob/master/src/main/java/org/sputnikdev/bluetooth/manager/BluetoothManager.java)). These are components which define lifecycle of BT objects and contain logic for error recovery. They are similar to the transport APIs, but yet different because they are active components, i.e. they implement some logic for each of BT objects (adapter, device, characteristic) that make the system more robust and enable the system to recover from unexpected situations such as disconnections and power outages.

Apart from making the system more stable, the Governors are designed in a such way that they can be used externally, in other words it is another abstraction layer which hides some specifics/difficulties of the BT protocol behind user-friendly APIs.

Obviously the Bluetooth Manager is supposed to help us with Robustness and Extensibility KPIs.

### 4. GATT Parser

It is a component which can translate BT raw data into user-friendly format. In short, it reads GATT specifications (xml files) and converts raw data into map of: Field name -> value. Works both ways, e.g. raw data -> fields (read operation) and fields -> raw data (write operation). All complex logic related to bit operations, type conversions, validations etc are hidden in this component. More info [here](https://github.com/sputnikdev/bluetooth-gatt-parser).

This component will allow us to create "generic" OH handlers which can work with any devices that conform GATT specifications OR users can supply their own implementations for GATT specs in XML files to add their own custom made devices.

## Architecture design

![openhab bluetooth](https://user-images.githubusercontent.com/1161883/28918674-63a82d6c-789e-11e7-86d7-a3e32e44921d.png)

As you can see that diagram reflects the diagram pointed out [earlier](https://github.com/eclipse/smarthome/pull/3531#issuecomment-305831722). It is quite similar. There are bundles to add transport, bluetooth binding APIs and some specific binding for a particular device (YeeLightBlue). 

The only significant difference is the way how transport is plugged in to OH. In contrast to @cdjackson implementation, the transport implementations are plugged in to Bluetooth Manager via well defined transport APIs.

Another difference is that all OH handlers are supposed to work with the "Governors API" which provides/separates handler specific logic from dealing with BT protocol nasties.

Please note that on the diagram green colour means that it is already implemented (well to certain extend that it can be thought that it is implemented), blue and red colours - something we need to develop/merge. In my mind, the blue components can be implemented/merged by @cdjackson and the red components by me. Of course we all need to put some effort in polishing the green components, however I promise to take the main responsibility in supporting the green stuff.
