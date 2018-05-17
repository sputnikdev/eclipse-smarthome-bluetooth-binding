package org.sputnikdev.esh.binding.bluetooth.handler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParser;
import org.sputnikdev.bluetooth.gattparser.BluetoothGattParserFactory;
import org.sputnikdev.bluetooth.manager.BluetoothManager;
import org.sputnikdev.bluetooth.manager.DeviceGovernor;
import org.sputnikdev.bluetooth.manager.GattCharacteristic;
import org.sputnikdev.bluetooth.manager.GattService;
import org.sputnikdev.bluetooth.manager.auth.PinCodeAuthenticationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PinCodeAuthenticationProvider.class, ExplicitPinCodeAuthProvider.class })
public class ExplicitPinCodeAuthProviderIntegrationTest {

    private static final URL DEVICE_URL = new URL("/11:22:33:44:55:66/11:22:33:44:55:66");

    private static final String XIAOMI_AUTH_SERVICE_UUID = "00001204-0000-1000-8000-00805f9b34fb";
    private static final String XIAOMI_AUTH_CHARACTERISTIC_UUID = "00001a00-0000-1000-8000-00805f9b34fb";
    private static final byte[] PIN_CODE_HEX = {(byte) 0xaa, (byte) 0xbb};
    private static final byte[] PIN_CODE_DECIMAL = {(byte) 0xd2, (byte) 0x04}; // 1234
    private static final URL XIAOMI_AUTH_CHARACTERISTIC_URL = DEVICE_URL.copyWith(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID);
    private static final URL XIAOMI_MIFLORA_CHARACTERISTIC_URL = XIAOMI_AUTH_CHARACTERISTIC_URL.getServiceURL()
            .copyWithCharacteristic("00001A01-0000-1000-8000-00805f9b34fb");

    private static final URL BATTERY_LEVEL_SERVICE_URL = DEVICE_URL.copyWith("0000180f-0000-1000-8000-00805f9b34fb",
            "00002a19-0000-1000-8000-00805f9b34fb");

    @Mock
    private BluetoothManager bluetoothManager;
    @Mock
    private DeviceGovernor deviceGovernor;

    private PinCodeAuthenticationProvider delegate = PowerMockito.mock(PinCodeAuthenticationProvider.class);

    @Mock
    private GattService authService;
    @Mock
    private GattService batteryLevelChracteristic;

    @Mock
    private GattCharacteristic authCharacteristic;
    @Mock
    private GattCharacteristic characteristic1;
    @Mock
    private GattCharacteristic batteryLevelCharacteristic;

    private BluetoothGattParser gattParser = BluetoothGattParserFactory.getDefault();

    private ExplicitPinCodeAuthProvider provider;

    @Before
    public void setUp() throws Exception {
        when(deviceGovernor.getURL()).thenReturn(DEVICE_URL);

        when(authService.getCharacteristics()).thenReturn(Arrays.asList(authCharacteristic, characteristic1));
        when(batteryLevelChracteristic.getCharacteristics()).thenReturn(Collections.singletonList(batteryLevelCharacteristic));

        List<GattService> resolvedServices = new ArrayList<>();
        resolvedServices.add(authService);
        resolvedServices.add(batteryLevelChracteristic);

        when(authService.getURL()).thenReturn(XIAOMI_AUTH_CHARACTERISTIC_URL.getServiceURL());
        when(batteryLevelChracteristic.getURL()).thenReturn(BATTERY_LEVEL_SERVICE_URL.getCharacteristicURL());

        when(authCharacteristic.getURL()).thenReturn(XIAOMI_AUTH_CHARACTERISTIC_URL);
        when(characteristic1.getURL()).thenReturn(XIAOMI_MIFLORA_CHARACTERISTIC_URL);
        when(batteryLevelCharacteristic.getURL()).thenReturn(BATTERY_LEVEL_SERVICE_URL);

        when(deviceGovernor.getResolvedServices()).thenReturn(resolvedServices);
    }

    @Test
    public void testAuthenticateHexArrayPinCode() throws Exception {

        PowerMockito.whenNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, PIN_CODE_HEX)
                .thenReturn(delegate);

        provider = new ExplicitPinCodeAuthProvider(gattParser, XIAOMI_AUTH_CHARACTERISTIC_UUID + "=[aa, bb]");

        provider.authenticate(bluetoothManager, deviceGovernor);

        PowerMockito.verifyNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, PIN_CODE_HEX);
        verify(delegate).authenticate(bluetoothManager, deviceGovernor);
    }

    @Test
    public void testAuthenticateDecimalPinCode() throws Exception {

        PowerMockito.whenNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, PIN_CODE_DECIMAL)
                .thenReturn(delegate);

        provider = new ExplicitPinCodeAuthProvider(gattParser, XIAOMI_AUTH_CHARACTERISTIC_UUID + "=1234");

        provider.authenticate(bluetoothManager, deviceGovernor);

        PowerMockito.verifyNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, PIN_CODE_DECIMAL);
        verify(delegate).authenticate(bluetoothManager, deviceGovernor);
    }

    @Test
    public void testAuthenticateNoPinCodeFound() throws Exception {

        provider = new ExplicitPinCodeAuthProvider(gattParser, "12345678-0000-1000-8000-00805f9b34fb=1234");

        provider.authenticate(bluetoothManager, deviceGovernor);

        verify(delegate, never()).authenticate(bluetoothManager, deviceGovernor);
    }

}
