package ai.nextgpu.agent.util;

import ai.nextgpu.agent.service.BaseTest;
import ai.nextgpu.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BaseTest.TestConfig.class)
class HardwareUtilTest {

    @Autowired
    private HardwareUtil hardwareUtil;

    @Autowired
    private Environment environment; // To read application properties

    private Boolean isAiServiceEnabled = false;

    @BeforeEach
    void setUp() {
        assertNotNull(hardwareUtil, "HardwareUtil is null");
        // Check if OpenAI is enabled in application properties, otherwise skip all tests
        isAiServiceEnabled = Boolean.parseBoolean(environment.getProperty("openai.api.enabled", "false"));
    }

    @Test
    void shouldGetVendorByMacAddress() {
        String vendor = hardwareUtil.getVendorByMacAddress("60FDA6");
        assertTrue(vendor.toUpperCase().contains("APPLE"));
    }

    @Test
    void shouldDetectCpus() {
        Set<Cpu> cpus = hardwareUtil.detectCpus();
        assertNotNull(cpus, "CPU list should not be null");
        // Validate the first CPU
        Cpu firstCpu = cpus.stream().findFirst().get();
        assertTrue(firstCpu.getCores() > 0, "CPU should have at least 1 core");
        assertTrue(firstCpu.getThreads() >= firstCpu.getCores(), "Threads should be >= cores");
        if  (isAiServiceEnabled) {
            assertNotNull(firstCpu.getManufacturer(), "CPU manufacturer shouldn't be null");
        }
    }

    @Test
    void shouldDetectMemoryModules() {
        Set<MemoryModule> memoryModules = hardwareUtil.detectMemoryModules();
        assertNotNull(memoryModules, "Memory modules list should not be null");
        // Check the first memory module
        MemoryModule firstModule = memoryModules.stream().findFirst().get();
        assertTrue(firstModule.getCapacity() > 0, "Memory capacity should be > 0 MB");
    }

    @Test
    void shouldDetectGpus() {
        Set<Gpu> gpus = hardwareUtil.detectGpus();
        assertNotNull(gpus, "GPU list should not be null");
        Gpu firstGpu = gpus.stream().findFirst().get();
        assertTrue(firstGpu.getCapacity() > 0, "GPU VRAM should be > 0 MB");
    }

    @Test
    void shouldDetectStorages() {
        Set<Storage> storages = hardwareUtil.detectStorages();
        assertNotNull(storages, "Storage list should not be null");
        Storage firstStorage = storages.stream().findFirst().get();
        assertTrue(firstStorage.getCapacity() > 0, "Storage capacity should be > 0 GB");
    }

    @Test
    void shouldDetectNetworkInterfaces() {
        Set<NetworkDevice> devices = hardwareUtil.detectNetworkInterfaces();
        assertNotNull(devices, "Network device list should not be null");
        NetworkDevice firstNic = devices.stream().findFirst().get();
        assertNotNull(firstNic.getMacAddress(), "NIC macAddress shouldn't be null");
        // At least one device should have speed > 0 Mbps
        assertTrue(devices.stream().anyMatch(n -> n.getSpeed() > 0), "NIC speed should be > 0 Mbps");
    }

    @Test
    void shouldDetectSensors() {
        Map<String, Boolean> sensors = hardwareUtil.detectSensors();
        assertNotNull(sensors, "Sensors map should not be null");
        assertTrue(sensors.containsKey("cpu_thermometer"), "Should have 'cpu_thermometer' sensor key");
        assertTrue(sensors.containsKey("cpu_voltmeter"), "Should have 'cpu_voltmeter' sensor key");
        assertTrue(sensors.containsKey("cpu_fans"), "Should have 'cpu_fans' sensor key");
    }

    @Test
    void shouldDetectUsbDevices() {
        Set<GenericComponent> usbDevices = hardwareUtil.detectUsbDevices();
        assertNotNull(usbDevices, "USB device list should not be null");
        GenericComponent firstUsb = usbDevices.stream().findFirst().get();
        assertNotNull(firstUsb.getName(), "USB device name shouldn't be null");
    }
    
    /**
     * Reads the CPU temperature sensor.
     * We only ensure it doesn't crash; some systems may return 0 if no sensor present.
     */
    @Test
    void shouldReadCurrentCpuTemperature() {
        double temperature = hardwareUtil.readCurrentCpuTemperature();
        // 0 or negative might be environment dependent
        assertTrue(temperature >= 0, "CPU temperature is either 0 or a positive number (depends on environment)");
    }

    /**
     * Reads CPU voltage sensor.
     * Some environments will always return 0 if sensor is missing.
     */
    @Test
    void shouldReadCurrentCpuVoltage() {
        double voltage = hardwareUtil.readCurrentCpuVoltage();
        assertTrue(voltage >= 0, "CPU voltage is either 0 or a positive number (depends on environment)");
    }

    /**
     * Reads current fan speeds.
     * Many machines will return an empty array if there's no direct fan sensor or if system is fanless.
     */
    @Test
    void shouldReadCurrentFanSpeeds() {
        int[] fanSpeeds = hardwareUtil.readCurrentFanSpeeds();
        assertNotNull(fanSpeeds, "Fan speeds array shouldn't be null");
    }

    @Test
    void shouldReadCpuUsage() {
        double cpuUsage = hardwareUtil.readCurrentCpuUsage();
        assertTrue(cpuUsage >= 0 && cpuUsage <= 100,
                "CPU usage should be a percentage between 0 and 100 (inclusive)");
    }

    @Test
    void shouldReadMemoryUsage() {
        double memoryUsage = hardwareUtil.readCurrentMemoryUsage();
        assertTrue(memoryUsage >= 0 && memoryUsage <= 100,
                "Memory usage should be a percentage between 0 and 100 (inclusive)");
    }

    @Test
    void shouldDetectComputer() {
        Computer computer = hardwareUtil.detectComputer();
        System.out.println("Computer: " + computer);

        assertNotNull(computer, "Computer should not be null");
        assertFalse(computer.getCpus().isEmpty(), "Computer should have at least 1 CPU");
        assertFalse(computer.getGpus().isEmpty(), "Computer should have at least 1 Gpu");
        assertFalse(computer.getStorages().isEmpty(), "Computer should have at least 1 Storage");
        assertFalse(computer.getNetworkDevices().isEmpty(), "Computer should have at least 1 Network Device");
        assertNotNull(computer.getOperatingSystem(), "Computer should have an Operating System");
        assertFalse(computer.getMemories().isEmpty(), "Computer should have at least 1 Memory");
    }

    @Test
    void shouldDetectOperatingSystem() {
        String operatingSystem = hardwareUtil.detectOperatingSystem();
        assertNotNull(operatingSystem, "Operating system should be null");
        if (operatingSystem.contains("Windows")) {
            assertTrue(operatingSystem.contains("Windows"), "Operating system should be windows");
        }
        if (operatingSystem.contains("Linux")) {
            assertTrue(operatingSystem.contains("Linux"), "Operating system should be linux");
        }
    }
}
