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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PinCodeAuthenticationProvider.class, AutomaticPinCodeAuthProvider.class })
public class AutomaticPinCodeAuthProviderIntegrationTest {

    private static final URL DEVICE_URL = new URL("/11:22:33:44:55:66/11:22:33:44:55:66");

    private static final String MINEW_AUTH_SERVICE_UUID = "0000eee1-0000-1000-8000-00805f9b34fb";
    private static final String MINEW_AUTH_CHARACTERISTIC_UUID = "0000eee3-0000-1000-8000-00805f9b34fb";
    private static final byte[] MINEW_PIN_CODE = {0x01, (byte) 0xc1, 0x61, 0x6d, 0x54, 0x76, 0x01, 0x6a};
    private static final byte[] MINEW_AUTH_SUCCESSFUL_RESPONSE = {0x6b, 0x65, 0x79, 0x20, 0x73, 0x75, 0x63};
    private static final URL MINEW_AUTH_CHARACTERISTIC_URL = DEVICE_URL.copyWith(MINEW_AUTH_SERVICE_UUID, MINEW_AUTH_CHARACTERISTIC_UUID);
    private static final URL MINEW_BUTTON_CHARACTERISTIC_URL = MINEW_AUTH_CHARACTERISTIC_URL.getServiceURL()
            .copyWithCharacteristic("0000eee2-0000-1000-8000-00805f9b34fb");

    private static final String XIAOMI_AUTH_SERVICE_UUID = "00001204-0000-1000-8000-00805f9b34fb";
    private static final String XIAOMI_AUTH_CHARACTERISTIC_UUID = "00001a00-0000-1000-8000-00805f9b34fb";
    private static final byte[] XIAOMI_PIN_CODE = {(byte) 0xA0, 0x1F};
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

    private AutomaticPinCodeAuthProvider provider = new AutomaticPinCodeAuthProvider(gattParser);

    @Before
    public void setUp() throws Exception {
        when(deviceGovernor.getURL()).thenReturn(DEVICE_URL);

        when(authService.getCharacteristics()).thenReturn(Arrays.asList(authCharacteristic, characteristic1));
        when(batteryLevelChracteristic.getCharacteristics()).thenReturn(Collections.singletonList(batteryLevelCharacteristic));

        List<GattService> resolvedServices = new ArrayList<>();
        resolvedServices.add(authService);
        resolvedServices.add(batteryLevelChracteristic);

        when(deviceGovernor.getResolvedServices()).thenReturn(resolvedServices);
    }

    @Test
    public void testAuthenticateWithAuthStatus() throws Exception {
        PowerMockito.whenNew(PinCodeAuthenticationProvider.class)
                .withArguments(MINEW_AUTH_SERVICE_UUID, MINEW_AUTH_CHARACTERISTIC_UUID, MINEW_PIN_CODE, MINEW_AUTH_SUCCESSFUL_RESPONSE)
                .thenReturn(delegate);

        mockAttributes(MINEW_AUTH_CHARACTERISTIC_URL, MINEW_BUTTON_CHARACTERISTIC_URL);

        provider.authenticate(bluetoothManager, deviceGovernor);

        PowerMockito.verifyNew(PinCodeAuthenticationProvider.class)
                .withArguments(MINEW_AUTH_SERVICE_UUID, MINEW_AUTH_CHARACTERISTIC_UUID, MINEW_PIN_CODE, MINEW_AUTH_SUCCESSFUL_RESPONSE);
        verify(delegate).authenticate(bluetoothManager, deviceGovernor);
    }

    @Test
    public void testAuthenticateWithoutAuthStatus() throws Exception {
        PowerMockito.whenNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, XIAOMI_PIN_CODE, null)
                .thenReturn(delegate);

        mockAttributes(XIAOMI_AUTH_CHARACTERISTIC_URL, XIAOMI_MIFLORA_CHARACTERISTIC_URL);

        provider.authenticate(bluetoothManager, deviceGovernor);

        PowerMockito.verifyNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, XIAOMI_PIN_CODE, null);
        verify(delegate).authenticate(bluetoothManager, deviceGovernor);
    }

    @Test
    public void testAuthenticateNoPinCodeFound() throws Exception {
        PowerMockito.whenNew(PinCodeAuthenticationProvider.class)
                .withArguments(XIAOMI_AUTH_SERVICE_UUID, XIAOMI_AUTH_CHARACTERISTIC_UUID, XIAOMI_PIN_CODE, null)
                .thenReturn(delegate);

        mockAttributes(MINEW_AUTH_CHARACTERISTIC_URL, MINEW_BUTTON_CHARACTERISTIC_URL);

        when(authService.getCharacteristics()).thenReturn(Collections.singletonList(characteristic1));

        provider.authenticate(bluetoothManager, deviceGovernor);

        verify(delegate, never()).authenticate(bluetoothManager, deviceGovernor);
    }

    private void mockAttributes(URL authURL, URL secondaryURL) {
        when(authService.getURL()).thenReturn(authURL.getServiceURL());
        when(batteryLevelChracteristic.getURL()).thenReturn(BATTERY_LEVEL_SERVICE_URL.getCharacteristicURL());

        when(authCharacteristic.getURL()).thenReturn(authURL);
        when(characteristic1.getURL()).thenReturn(secondaryURL);
        when(batteryLevelCharacteristic.getURL()).thenReturn(BATTERY_LEVEL_SERVICE_URL);
    }

}
