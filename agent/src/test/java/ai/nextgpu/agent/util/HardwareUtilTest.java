package ai.nextgpu.agent.util;

import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.nextgpu.agent.service.BaseTest;
import ai.nextgpu.common.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest(classes = BaseTest.TestConfig.class)
class HardwareUtilTest {
    private static final Logger log = LoggerFactory.getLogger(HardwareUtilTest.class);

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

    @SuppressWarnings("unchecked")
    private List<Cpu> invokeReadCpusInfoFromOS() throws Exception {
        Method method = HardwareUtil.class.getDeclaredMethod("readCpusInfoFromOS");
        method.setAccessible(true);
        return (List<Cpu>) method.invoke(hardwareUtil);
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
        log.debug("Computer: {}", computer);

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

    @Test
    void parsesWmicSingleCpu() throws Exception {
        OSUtil.IS_WINDOWS = true;
        OSUtil.IS_LINUX = false;

        String wmic = String.join("\r\n",
                "AddressWidth=64",
                "Architecture=9",
                "Manufacturer=AuthenticAMD",
                "MaxClockSpeed=3801",
                "Name=AMD Ryzen 7 5700G with Radeon Graphics",
                "NumberOfCores=8",
                "NumberOfLogicalProcessors=16",
                "L3CacheSize=16384",
                "ProcessorId=178BFBFF00A50F00",
                "");

        try (MockedStatic<OSUtil> os = mockStatic(OSUtil.class)) {
            os.when(() -> OSUtil.executeCommand("wmic cpu get /format:list")).thenReturn(wmic);

            List<Cpu> cpus = invokeReadCpusInfoFromOS();

            assertEquals(1, cpus.size(), "One block should yield one CPU");
            Cpu cpu = cpus.getFirst();
            assertEquals(8, cpu.getCores().intValue());
            assertEquals(16, cpu.getThreads().intValue());
            assertEquals(3801, cpu.getMaxClock().intValue());
            assertEquals(16384, cpu.getL3Cache().intValue());
            assertEquals(CpuArchitecture.X86_64, cpu.getArchitecture());
            assertEquals("AuthenticAMD", cpu.getManufacturer());
            assertEquals("178BFBFF00A50F00", cpu.getProductIdentifier());
            // model = Name; name = Manufacturer + " " + Name
            assertEquals("AMD Ryzen 7 5700G with Radeon Graphics", cpu.getModel());
            assertEquals("AuthenticAMD AMD Ryzen 7 5700G with Radeon Graphics", cpu.getName());
            // wmic has no min clock; it is sourced from OSHI, which is either unknown (null) or positive.
            assertTrue(cpu.getMinClock() == null || cpu.getMinClock() > 0);
        }
    }

    @Test
    void parsesWmicMatchesKeysCaseInsensitively() throws Exception {
        OSUtil.IS_WINDOWS = true;
        OSUtil.IS_LINUX = false;

        // Same data with mangled key casing; parsing must still resolve every field.
        String wmic = String.join("\r\n",
                "architecture=9",
                "MANUFACTURER=GenuineIntel",
                "maxclockspeed=2400",
                "name=Intel(R) Core(TM) i7-9750H",
                "numberofcores=6",
                "NumberOfLogicalProcessors=12",
                "l3cachesize=12288",
                "");

        try (MockedStatic<OSUtil> os = mockStatic(OSUtil.class)) {
            os.when(() -> OSUtil.executeCommand("wmic cpu get /format:list")).thenReturn(wmic);

            Cpu cpu = invokeReadCpusInfoFromOS().getFirst();
            assertEquals(6, cpu.getCores().intValue());
            assertEquals(12, cpu.getThreads().intValue());
            assertEquals(2400, cpu.getMaxClock().intValue());
            assertEquals(12288, cpu.getL3Cache().intValue());
            assertEquals("GenuineIntel", cpu.getManufacturer());
            assertEquals(CpuArchitecture.X86_64, cpu.getArchitecture());
            assertEquals("GenuineIntel Intel(R) Core(TM) i7-9750H", cpu.getName());
        }
    }

    @Test
    void parsesWmicMultipleSockets() throws Exception {
        OSUtil.IS_WINDOWS = true;
        OSUtil.IS_LINUX = false;

        String block = String.join("\r\n",
                "Architecture=9",
                "Manufacturer=GenuineIntel",
                "MaxClockSpeed=2100",
                "Name=Intel(R) Xeon(R) Gold 6130",
                "NumberOfCores=16",
                "NumberOfLogicalProcessors=32",
                "L3CacheSize=22528",
                "ProcessorId=ABCD");
        String wmic = block + "\r\n\r\n" + block + "\r\n";

        try (MockedStatic<OSUtil> os = mockStatic(OSUtil.class)) {
            os.when(() -> OSUtil.executeCommand("wmic cpu get /format:list")).thenReturn(wmic);

            List<Cpu> cpus = invokeReadCpusInfoFromOS();
            assertEquals(2, cpus.size(), "Two blocks should yield two CPU packages");
        }
    }

    // ------------------------------------------------------------------ Linux / lscpu

    @Test
    void parsesLscpuOutput() throws Exception {
        OSUtil.IS_WINDOWS = false;
        OSUtil.IS_LINUX = true;

        String lscpu = """
                {
                  "lscpu": [
                    { "field": "Architecture:", "data": "x86_64" },
                    { "field": "CPU(s):", "data": "12" },
                    { "field": "Vendor ID:", "data": "AuthenticAMD",
                      "children": [
                        { "field": "Model name:", "data": "AMD Ryzen 7 5700G with Radeon Graphics",
                          "children": [
                            { "field": "CPU family:", "data": "25" },
                            { "field": "Model:", "data": "80" },
                            { "field": "Thread(s) per core:", "data": "2" },
                            { "field": "Core(s) per socket:", "data": "6" },
                            { "field": "Socket(s):", "data": "1" },
                            { "field": "Stepping:", "data": "0" },
                            { "field": "CPU max MHz:", "data": "4672.0693" },
                            { "field": "CPU min MHz:", "data": "400.0000" }
                          ]
                        }
                      ]
                    },
                    { "field": "Caches (sum of all):", "data": null,
                      "children": [
                        { "field": "L3:", "data": "16 MiB (1 instance)" }
                      ]
                    }
                  ]
                }
                """;

        try (MockedStatic<OSUtil> os = mockStatic(OSUtil.class)) {
            os.when(() -> OSUtil.executeCommand("lscpu --json --output-all")).thenReturn(lscpu);

            List<Cpu> cpus = invokeReadCpusInfoFromOS();

            assertEquals(1, cpus.size(), "One socket should yield one CPU");
            Cpu cpu = cpus.getFirst();
            assertEquals(6, cpu.getCores().intValue(), "cores = cores-per-socket * sockets");
            assertEquals(12, cpu.getThreads().intValue(), "threads = total logical CPUs");
            assertEquals(4672, cpu.getMaxClock().intValue());
            assertEquals(400, cpu.getMinClock().intValue(), "min clock should come from lscpu when present");
            assertEquals(16384, cpu.getL3Cache().intValue(), "16 MiB -> 16384 KB");
            assertEquals(CpuArchitecture.X86_64, cpu.getArchitecture());
            assertEquals("AuthenticAMD", cpu.getManufacturer());
            assertEquals("AMD Ryzen 7 5700G with Radeon Graphics", cpu.getModel());
            assertEquals("AuthenticAMD AMD Ryzen 7 5700G with Radeon Graphics", cpu.getName());
            assertEquals("Family 25 Model 80 Stepping 0", cpu.getProductIdentifier());
        }
    }

    @Test
    void parsesLscpuEmitsOneCpuPerSocket() throws Exception {
        OSUtil.IS_WINDOWS = false;
        OSUtil.IS_LINUX = true;

        String lscpu = """
                {
                  "lscpu": [
                    { "field": "Architecture:", "data": "x86_64" },
                    { "field": "CPU(s):", "data": "64" },
                    { "field": "Vendor ID:", "data": "GenuineIntel",
                      "children": [
                        { "field": "Model name:", "data": "Intel(R) Xeon(R) Gold 6130",
                          "children": [
                            { "field": "Core(s) per socket:", "data": "16" },
                            { "field": "Socket(s):", "data": "2" }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        try (MockedStatic<OSUtil> os = mockStatic(OSUtil.class)) {
            os.when(() -> OSUtil.executeCommand("lscpu --json --output-all")).thenReturn(lscpu);

            List<Cpu> cpus = invokeReadCpusInfoFromOS();
            assertEquals(2, cpus.size(), "Two sockets should yield two CPU packages");
            assertEquals(32, cpus.getFirst().getCores().intValue(), "16 cores/socket * 2 sockets");
        }
    }
}
