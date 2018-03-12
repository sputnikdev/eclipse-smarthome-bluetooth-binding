# Xiaomi devices

## Table of contents

* [Important note for all Xiaomi devices](#important-note-for-all-xiaomi-devices)
* [Mi Flora plant sensor](#mi-flora-plant-sensor)
* [Temperature and Humidity sensor](#temperature-and-humidity-sensor)
* [Mi Smart Scale](#mi-smart-scale)
* [Mi Smart Kettle](#mi-smart-kettle)

The following devices have been tested with the Bluetooth Binding. Some other devices might also work with the binding out of the box.

## Important note for all Xiaomi devices

Xiaomi uses its own proprietary authentication protocol in their devices. The protocol logic is encapsulated into a native shared library (Android JNI library)
that is used to generate/encode/decode authentication tokens which must be exchanged with the device. Unfortunately, it is still unknown what algorithms are used, 
therefore it is not possible to establish a stable connection with Xiaomi devices. Xiaomi devices only allow a very short lived connection while it is expecting 
to receive an authentication token. Once authentication fails, device drops its connection. Fortunately, some Xiaomi devices actively advertise their data which 
can be easily received and decoded by the binding. More info on the Xiaomi authentication protocol can be found [here](https://github.com/sputnikdev/eclipse-smarthome-bluetooth-binding/issues/19#issuecomment-361113092).  

Make sure that the "**Discovering control**" on your adapter is **enabled**. Only when enabled, your adapter can receive advertising packets from bluetooth devices.

![ESH Bluetooth adapter](binding-adapter.png?raw=true "ESH Bluetooth adapter")

Keep in mind that bluetooth devices advertise their data only periodically, this means that not all channels will be discovered instantly. 
Allow 10-15 seconds for the binding to discover all channels. Once a device is added, you may see only some system channels like "Connected", "Connection control" etc. 
During the next 10-15 seconds, the binding gradually discovers advertising data and creates corresponding channels. 
You may want to refresh PaperUI page in your browser after 15s in order to see discovered channel. 

Although a short connection can be still established to read some data (actually some Xiaomi devices also require to enter a pin code, otherwise data is corrupted), 
it is **not recommended** to enable "Connection control" as this will cause endless connections/disconnections. 

## Mi Flora plant sensor

This sensor provides readings for soil moisture, soil fertility, ambient light and ambient temperature.

![Xiaomi MiFlora](xiaomi-miflora.png?raw=true "Xiaomi MiFlora")

### Important

Older firmware versions do not advertise any meaningful data, therefore a firmware upgrade to the newest version is required. This can be done in the official "Flower Care" app.

The most recent version at the time of writing this article is 3.1.8. Most likely you will need to upgrade firmware if you just purchased your sensor on Aliexpress or similar online shop 
as they are all shipped with an old firmware.

### Device discovery

It should be detected automatically, once a MiFlora device is in a range of a bluetooth adapter. An item in the PaperUI inbox should be created accordingly:

![Xiaomi MiFlora discovery](xiaomi-miflora-discovered.png?raw=true "Xiaomi MiFlora discovery")

Once all channels are discovered, the device "thing" should look like that:

![Xiaomi MiFlora thing](xiaomi-miflora-thing.png?raw=true "Xiaomi MiFlora thing")

### Device channels

As you can see from the pic above, MiFlora advertises 4 channels:

* Sunlight (ambient light)
* Moisture
* Fertility
* Temperature

All these channels correspond to what is displayed in the official "Flower Care" app and get refreshed roughly every 10-15 seconds.

## Temperature and Humidity sensor

This sensor provides readings for temperature and humidity.

![Xiaomi Temperature and Humidity](xiaomi-temp-and-hum.png?raw=true "Xiaomi Temperature and Humidity")

### Device discovery

It should be detected automatically, once a device is in a range of a bluetooth adapter. An item in the PaperUI inbox should be created accordingly:

![Xiaomi Temperature and Humidity discovery](xiaomi-temp-and-hum-discovery.png?raw=true "Xiaomi Temperature and Humidity discovery")

Once all channels are discovered, the device "thing" should look like that:

![Xiaomi Temperature and Humidity thing](xiaomi-temp-and-hum-thing.png?raw=true "Xiaomi Temperature and Humidity thing")

### Device channels

The device advertises 3 channels (refreshed every 5-10 seconds):

* Temperature
* Humidity
* Battery level

## Mi Smart Scale

Measures the smallest weight changes with a high-precision G-shaped sensor made of manganese steel.

![Mi Smart Scale thing](xiaomi-mi-smart-scale.png?raw=true "Mi Smart Scale")

### Device discovery

It should be detected automatically, once a Mi Scale device is in a range of a bluetooth adapter. An item in the PaperUI inbox should be created accordingly.

![Mi Smart Scale thing](xiaomi-mi-smart-scale-thing.png?raw=true "Mi Smart Scale thing")

### Device channels

Mi Smart Scale provides the following channels:

* Weight (either in lb or kg)
* The time of the last measurement (split on 6 channels for each time component: Year, Month, Day, Hour, Minute, Second)

## Mi Smart Kettle

//TODO