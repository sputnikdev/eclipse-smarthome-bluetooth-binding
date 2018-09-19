## Installation

The Bluetooth binding can be installed via PaperUI and Eclipse MarketPlace.

1. The Eclipse MarketPlace add-on should be installed first (it might be a bit confusing but the MarketPlace add-on is listed as "Eclipse IoT Market"):
![Eclipse MarketPlace installation](eclipse-iot-install.png?raw=true "Eclipse MarketPlace installation")
2. Once the MarketPlace add-on is installed it should be configured to display "betta" and "alpha" listings:
![Eclipse MarketPlace configuration](eclipse-iot-configure.png?raw=true "Eclipse MarketPlace configuration")
3. You may also want to enable the "Item Linking - Simple Mode" to automatically create thing items:
![ESH Enabling Simple Linking](esh-simple-linking.png?raw=true "ESH Enabling Simple Linking")
4. Depending on what bluetooth adapter you have you will need to install one of the bluetooth transport extensions
(or all of them if you have both types of bluetooth adapters). See [the compatibility matrix](README.md#bluetooth-adapters-compatibility-matrix).
![Eclipse MarketPlace Bluetooth Transport installation](eclipse-iot-bluetooth-transport.png?raw=true "Eclipse MarketPlace Bluetooth Transport installation")
**IMPORTANT:** There are some certain prerequisites that must be met for the generic adapter type (TinyB Bluetooth Transport) to work properly.
Please follow [these steps](https://github.com/sputnikdev/bluetooth-manager-tinyb#prerequisites) to find out if you need to do anything special.
5. Finally install the Bluetooth Binding:
![ESH Bluetooth Binding installation](eclipse-iot-bluetooth-binding.png?raw=true "ESH Bluetooth Binding installation")