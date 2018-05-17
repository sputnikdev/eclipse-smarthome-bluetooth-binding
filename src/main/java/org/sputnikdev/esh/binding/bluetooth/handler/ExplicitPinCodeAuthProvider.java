package org.sputnikdev.esh.binding.bluetooth.handler;

import org.apache.commons.lang.ArrayUtils;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExplicitPinCodeAuthProvider implements AuthenticationProvider {

    private static final Pattern PIN_CODE_DEFINITION_REGEX = Pattern.compile(
            "^(?<uuid>[0-9A-Fa-f]{4,8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12})"
                    + "=((?<decimal>\\d+)|(?<hex>\\[[0-9A-Fa-f]{2}(,\\s?[0-9A-Fa-f]{2}){0,19}\\]))$");

    private Logger logger = LoggerFactory.getLogger(ExplicitPinCodeAuthProvider.class);

    private PinCodeAuthenticationProvider delegate;

    private String characteristicUUID;
    private byte[] pinCode;

    public ExplicitPinCodeAuthProvider(BluetoothGattParser gattParser, String pinCodeDefinition) {
        Matcher matcher = PIN_CODE_DEFINITION_REGEX.matcher(pinCodeDefinition);
        if (matcher.find()) {
            characteristicUUID = matcher.group("uuid").toLowerCase();
            String decimalCode = matcher.group("decimal");
            if (decimalCode != null) {
                pinCode = new BigInteger(decimalCode).toByteArray();
                ArrayUtils.reverse(pinCode);
            } else {

                pinCode = gattParser.serialize(matcher.group("hex"), 16);
            }
        } else {
            throw new IllegalStateException("Invalid pin code definition: " + pinCodeDefinition);
        }
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

    void discoverPinCodeCharacteristic(URL url, List<GattService> services) {
        GattCharacteristic characteristic = services.stream().flatMap(service -> service.getCharacteristics().stream())
                .filter(chr -> chr.getURL().getCharacteristicUUID().toLowerCase().equals(characteristicUUID))
                .findAny().orElse(null);

        if (characteristic == null) {
            logger.warn("Pin code characteristic is not found. Authentication won't be performed: {}", url);
            return;
        }
        logger.debug("Pin code characteristic found: {}", url);

        URL pinCodeCharacteristic = characteristic.getURL();

        delegate = new PinCodeAuthenticationProvider(pinCodeCharacteristic.getServiceUUID(),
                pinCodeCharacteristic.getCharacteristicUUID(), pinCode);

    }

}
