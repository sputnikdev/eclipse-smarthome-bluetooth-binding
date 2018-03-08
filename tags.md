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

## Available channels

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
4. **Online**. The device "thing" also provides the standard "Online" channel that is used to detect presence of the device. 
    Online timeout is configured via the "thing" settings in OH.

## Usage

Here is the understanding of how iTag devices work:

* When a tag is powered up, it beeps (double beep). Then it starts broadcasting (just only RSSI and its name).
* From here, there are some options:
    1. Disable “Connection control” (leave the OH “thing” to be disconnected). This means you can’t use neither the button nor the 
    alarm. You just use it as a standard beacon, e.g. you can only detect presence.
    2. Enable “Connection control” (make the OH "thing" it to be connected). This means: 
        * The device “remembers” your adapter. Once it gets out of the range of the adapter, it will beep for a minute 
        (slow beeping). When it appears in the range of the adapter again, it gets connected automatically (single beep). 
        Also when connected to a new adapter, it forgets the previous one (or a previously connected mobile phone).
        * You can use the button to trigger events in OH
        * You can trigger an alarm on your iTag

**Note for the option #1.** Bear in mind that the iTag device always remembers previous connected client (an adapter or a mobile phone), 
this means that if you have never enabled “Connection control” and it has never been connected to the binding, 
the option #2 applies to your phone (or previously connected adapter).

PS. from the author:
I personally find it a bit annoying when it starts beeping once it looses its connection. You have to push the button to stop beeping. 
That's why I use iTags mainly as option #1. When I loose my keys in my house, I still can enable the “Connection control” and trigger 
the alarm to find them. This means that most of the time my iTag devices stay disconnected and used as beacons only.

TODO. Need to check what happens if a disconnection is triggered by the binding, e.g. will it beep for a min? It might 
be that if the device detects that the client drops its connection on purpose (not because of link loss), then it might 
“unlink” itself and forget the client.