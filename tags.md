# Bluetooth tracking tags (beacons)

Bluetooth tracking tags are small beacon devices that normally attached to a keyring.

![iTag](itag.png?raw=true "iTag")

There are a number of tracking tags on Aliexpress for a very cheap price. The following tags have been tested with the binding:

* [Dehyaton](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/21)
* [Elistooop](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/20)

Essentially these are "iTag" devices that are based on the same hardware and have the same GATT interfaces.

You may find that some other tags work with the binding out of the box.

# iTag

## Discovery

Normally these devices are identified automatically by the binding as a "ble" device. Once added in OpenHab/SmartHome they look like that:

![iTag thing](itag-thing.png?raw=true "iTag thing")

## Usage

There are 3 channels provided by iTag devices when a device is connected ("Connection control" is enabled):

1. **Battery Level**. This channel represents a standard GATT characteristic - battery level percentage.
2. **Alert Level**. This channel represents a standard GATT characteristic - alert level (Link loss service). 
    It can be used to trigger alerts on the device, e.g. to make it "beeping". In PaperUI click on the channel and 
    select either "Mild Alert" or "High Alert" (no difference between these two) to start beeping.
    
    ![iTag alert](itag-alert.png?raw=true "iTag alert")
    
3. **Button**. This channel represents a custom non-standard characteristic. When button is pressed on the device, 
    the channel gets triggered with the "boolean" true value. Please note, once the channel is triggered, 
    the value does not get reverted to "false". This means that when implementing a rule for the button in, 
    make sure you detect "any" changes of that channel.
    
Apart from these 3 specific channels, the device "thing" also provides the standard "Online" channel that is used 
to detect presence of the device. 