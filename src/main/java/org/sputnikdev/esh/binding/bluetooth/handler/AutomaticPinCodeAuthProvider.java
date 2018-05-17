package org.sputnikdev.esh.binding.bluetooth.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.FieldHolder;
import org.sputnikdev.bluetooth.gattparser.GattRequest;
import org.sputnikdev.bluetooth.gattparser.spec.Enumeration;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattCharacteristic;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.auth.AuthenticationProvider;
import org.sputnikdev.bluetooth.manager.auth.PinCodeAuthenticationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutomaticPinCodeAuthProvider implements AuthenticationProvider {

    private Logger logger = LoggerFactory.getLogger(AutomaticPinCodeAuthProvider.class);

    private PinCodeAuthenticationProvider delegate;

    private BluetoothGattParser gattParser;
    private URL pinCodeCharacteristic;
    private byte[] pinCode;
    private byte[] defaultPinCode;
    private byte[] authorisedCode;

    public AutomaticPinCodeAuthProvider(BluetoothGattParser gattParser) {
        this.gattParser = gattParser;
    }

    @Override
    public void authenticate(BluetoothManager bluetoothManager, DeviceGovernor governor) {
        if (delegate == null) {
            discoverPinCodeCharacteristic(governor.getURL(), governor.getResolvedServices());
        }

        if (delegate != null) {
            delegate.authenticate(bluetoothManager, governor);
        }
    }

    public byte[] getPinCode() {
        return Arrays.copyOf(pinCode, pinCode.length);
    }

    public void setPinCode(byte[] pinCode) {
        this.pinCode = Arrays.copyOf(pinCode, pinCode.length);
    }

    public byte[] getDefaultPinCode() {
        return Arrays.copyOf(defaultPinCode, pinCode.length);
    }

    void discoverPinCodeCharacteristic(URL url, List<GattService> services) {
        List<GattCharacteristic> chars = services.stream().flatMap(service -> service.getCharacteristics().stream())
                .filter(characteristic ->
                        gattParser.isKnownCharacteristic(characteristic.getURL().getCharacteristicUUID()))
                .collect(Collectors.toList());

        GattCharacteristic pinCodeSpec = findCharacteristicByFieldName(url, chars, "Pin Code");
        if (pinCodeSpec == null) {
            logger.debug("Pin code characteristic is not found. Authentication won't be performed: {}", url);
            return;
        }

        pinCodeCharacteristic = pinCodeSpec.getURL();

        defaultPinCode = findPinCodeValue(pinCodeSpec);
        authorisedCode = findAuthStatusValue(pinCodeSpec);

        if (defaultPinCode != null) {
            delegate = new PinCodeAuthenticationProvider(pinCodeCharacteristic.getServiceUUID(),
                    pinCodeCharacteristic.getCharacteristicUUID(), defaultPinCode, authorisedCode);
        } else {
            logger.warn("Pin code value is not found. Authentication won't be performed. {}", pinCodeCharacteristic);
        }
    }

    private GattCharacteristic findCharacteristicByFieldName(URL url,
                                                             List<GattCharacteristic> chars, String fieldName) {
        List<GattCharacteristic> targetChars = new ArrayList<>();
        for (GattCharacteristic ch : chars) {
            if (gattParser.getFields(ch.getURL().getCharacteristicUUID()).stream()
                    .filter(field -> fieldName.equalsIgnoreCase(field.getName())).count() > 0) {
                targetChars.add(ch);
            }
        }

        if (targetChars.isEmpty()) {
            logger.debug("No any '{}' field found in any characteristic: {}", fieldName, url);
            return null;
        } else if (targetChars.size() > 1) {
            logger.debug("More than one '{}' field found: {}. Using the first one: {}", fieldName,
                    targetChars.stream().map(ch -> ch.getURL().toString()).collect(Collectors.joining(", ")),
                    targetChars.get(0).getURL());
        } else {
            logger.debug("Field '{}' found in characteristic: {}", fieldName, targetChars.get(0).getURL());
        }
        return targetChars.get(0);
    }

    private Enumeration findEnumKeyByValue(GattRequest request, String fieldName, String enumValue) {
        if (!request.hasFieldHolder(fieldName)) {
            logger.warn("Could not find '{}' field", fieldName);
            return null;
        }
        FieldHolder holder = request.getFieldHolder(fieldName);
        List<Enumeration> enums = holder.getField().getEnumerations(enumValue);
        if (enums.isEmpty()) {
            logger.warn("Field '{}' field found, but it does not contain '{}' enum", fieldName, enumValue);
            return null;
        } else if (enums.size() > 1) {
            logger.warn("Field '{}' field found, but multiple '{}' enum values present. Using the first one: {}",
                    fieldName, enumValue, enums.get(0).getKey());
        } else {
            logger.warn("Field '{}' enum '{}' found", fieldName, enumValue);
        }
        return enums.get(0);
    }

    private byte[] findPinCodeValue(GattCharacteristic pinCodeSpec) {
        GattRequest request = gattParser.prepare(pinCodeSpec.getURL().getCharacteristicUUID());
        Enumeration pinCodeEnum = findEnumKeyByValue(request, "Pin Code", "PIN_CODE");
        if (pinCodeEnum == null) {
            logger.debug("Could not find pin code value: {}", pinCodeSpec);
            return null;
        }
        request.setField("Pin Code", pinCodeEnum);
        return gattParser.serialize(request, false);
    }

    private byte[] findAuthStatusValue(GattCharacteristic pinCodeSpec) {
        GattRequest request = gattParser.prepare(pinCodeSpec.getURL().getCharacteristicUUID());
        Enumeration authorisedEnum = findEnumKeyByValue(request, "Auth Status", "AUTHORISED");
        if (authorisedEnum == null) {
            logger.debug("Could not find 'authorised status' value: {}", pinCodeSpec);
            return null;
        }
        request.setField("Auth Status", authorisedEnum);
        return gattParser.serialize(request, false);
    }
}
