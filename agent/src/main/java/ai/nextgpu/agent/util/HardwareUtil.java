package ai.nextgpu.agent.util;

import ai.nextgpu.agent.aop.Loggable;
import ai.nextgpu.agent.service.NextGpuAgentService;
import ai.nextgpu.common.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for detecting and retrieving hardware details.
 * This class uses the OSHI library for low-level hardware queries and integrates with AIService to fetch additional specifications.
 */
@Component
public class HardwareUtil {

    private static final Logger log = LoggerFactory.getLogger(HardwareUtil.class);

    // Executor pool is fixed to 2 threads, as it will be used in deep cleaning
    private static final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private static final AtomicBoolean isDeepCleaning = new AtomicBoolean(false);

    // Cache of previous CPU tick counts to compute usage over time between calls
    private volatile long[] lastCpuTicks;

    // This file contains prefixes of MAC addresses and their respective vendors
    private static final String NIC_PROVIDERS_FILENAME = "nic-providers.tsv";

    // An external API to fetch Vendor by MAC prefix
    private static final String MAC_VENDOR_API = "https://api.macvendors.com/";

    private static final Map<String, String> VENDOR_MAC_MAP = new HashMap<>();

    static {
        loadOUIData(); // Load the OUI data at startup
    }

    @Value("${nextgpu.web.baseUrl:http://localhost:8080}")
    private String BASE_URL;

    @Autowired
    private HttpUtil httpUtil;

    /**
     * Internal method to return Hardware information using OSHI library
     */
    private HardwareAbstractionLayer detectHardwareSpecifications() {
        SystemInfo systemInfo = new SystemInfo();
        return systemInfo.getHardware();
    }

    /**
     * Loads OUI data from a TSV file containing NIC vendor prefixes.
     * This data is stored in a static map for fast lookups.
     */
    @Loggable
    private static void loadOUIData() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(NIC_PROVIDERS_FILENAME).getInputStream(), StandardCharsets.UTF_8))) {

            // Read TSV file with header
            CSVParser parser = new CSVParser(reader, CSVFormat.TDF.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build());

            // Load all records into the map for faster operation
            for (CSVRecord record : parser) {
                String mac = record.get("address") != null ? record.get("address").trim().toUpperCase() : "";
                String vendor = record.get("vendor") != null ? record.get("vendor").trim() : "";
                if (!mac.isEmpty() && !vendor.isEmpty()) {
                    VENDOR_MAC_MAP.put(mac, vendor);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read NIC Providers file", e);
        }
    }

    /**
     * Retrieves the vendor name of a given MAC address.
     * It first checks a local OUI map and falls back to MacVendors API if needed.
     *
     * @param macAddress The MAC address (format: "00:1A:2B:3C:4D:5E")
     * @return The vendor name or "Unknown Vendor" if not found.
     */
    @Loggable
    public String getVendorByMacAddress(String macAddress) {
        String unknown = "Unknown Vendor";
        if (macAddress == null || macAddress.isEmpty()) {
            return unknown;
        }
        String cleanedMacAddress = macAddress.replaceAll("[:\\-]", "").toUpperCase();
        if (cleanedMacAddress.length() < 6) {
            return unknown;
        }
        String macPrefix = cleanedMacAddress.substring(0, 6);

        // Check in local map
        if (VENDOR_MAC_MAP.containsKey(macPrefix)) {
            return VENDOR_MAC_MAP.get(macPrefix);
        }

        // Fallback to MacVendors API
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MAC_VENDOR_API + macPrefix))
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : unknown;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to identify NIC vendor", e);
            return unknown;
        }
    }

    /**
     * Returns a list of Processors installed in the computer. For now, the method assumes that if there are
     * multiple processors installed, they would be of exactly the same model.
     *
     * @return List of {@link Cpu} objects
     */
    public Set<Cpu> detectCpus() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        CentralProcessor processor = hardware.getProcessor();

        // Get the number of CPU packages (physical chips)
        int packageCount = processor.getPhysicalPackageCount();
        if (packageCount == 0) {
            log.error("No CPU detected.");
            return Collections.emptySet();
        }

        // Initialize CPU object based on the first processor
        Cpu baseCpu = Cpu.builder()
                .cores(processor.getPhysicalProcessorCount())
                .threads(processor.getLogicalProcessorCount())
                .architecture(CpuArchitecture.UNKNOWN)
                .maxClock((int) (processor.getMaxFreq() / 1_000_000)) // Convert Hz to MHz
                .build();

        CentralProcessor.ProcessorIdentifier identifier = processor.getProcessorIdentifier();
        baseCpu.setManufacturer(identifier.getVendor());
        baseCpu.setName(identifier.getName());
        baseCpu.setProductIdentifier(identifier.getProcessorID());
        baseCpu.setArchitecture(identifyCpuArchitecture(baseCpu.getName() + "; " + identifier.getFamily()));
        baseCpu.setDescription(processor.toString());

        // Get L3 Cache (take the highest level cache available)
        List<ProcessorCache> caches = processor.getProcessorCaches();
        caches.stream().max(Comparator.comparingInt(ProcessorCache::getLevel)).ifPresent(
                highestCache -> baseCpu.setL3Cache(highestCache.getCacheSize() / 1024)
        );

        // Fetch missing specifications from AIService only once
        try {
            String context = "You are an encyclopedia of computer hardware. Your responses are always a valid JSON Object.";
            String prompt = String.format(
                    "Provide specifications of CPU: %s %s. " +
                            "Do not convert value units, like Kilo, Mega or Giga. Keep them as-is. " +
                            "Model should only mention the series and model, not the vendor, e.g. Core i5 12600K, Ryzen 9 5900x, M4 Pro",
                    baseCpu.getManufacturer(), baseCpu.getName());

            Map<String, String> schemaMap = Map.of(
                    "model", "string",
                    "idDiscontinued", "boolean",
                    "tdp", "integer",
                    "minClock", "integer",
                    "yearReleased", "integer"
            );

            Map<String, Object> response = httpUtil.getStructuredAiResponse(BASE_URL, context, prompt, schemaMap);
            baseCpu.setModel((String) response.get("model"));
            baseCpu.setIsDiscontinued((Boolean) response.get("idDiscontinued"));
            baseCpu.setTdpWatts((Integer) response.get("tdp"));
            baseCpu.setMinClock((Integer) response.get("minClock"));
            baseCpu.setYearReleased((Integer) response.get("yearReleased"));

        } catch (Exception e) {
            log.error("Failed to generate prompt response!", e.getMessage());
        }

        // Generate a list of CPUs by cloning base CPU
        Set<Cpu> cpus = new HashSet<>();
        for (int i = 0; i < packageCount; i++) {
            Cpu cpuClone = baseCpu.toBuilder().build(); // Clone
            cpuClone.setName(baseCpu.getName());
            cpuClone.setProductIdentifier(baseCpu.getProductIdentifier()); // Identifier will be different for each CPU
            cpuClone.setManufacturer(baseCpu.getManufacturer());
            cpuClone.setModel(baseCpu.getModel());
            cpuClone.setYearReleased(baseCpu.getYearReleased());
            cpuClone.setIsDiscontinued(baseCpu.getIsDiscontinued());
            cpuClone.setTdpWatts(baseCpu.getTdpWatts());
            cpuClone.setDescription(baseCpu.getDescription());
            cpus.add(cpuClone);
        }
        return cpus;
    }


    /**
     * Returns the CPU Architecture by CPU name. The method tries to detect based on certain strings that may be part of the CPU name
     *
     * @param cpuName should be fully qualified name of CPU
     * @return CpuArchitecture enum
     */
    private CpuArchitecture identifyCpuArchitecture(String cpuName) {
        if (cpuName == null || cpuName.isEmpty()) {
            return CpuArchitecture.UNKNOWN;
        }
        String lower = cpuName.toLowerCase();
        // Match Intel or AMD
        if (lower.contains("intel") || lower.contains("amd")) {
            return CpuArchitecture.X86_64;
        }
        // Match Apple Silicon or Qualcomm Snapdragon
        if (lower.contains("apple") || lower.contains("snapdragon")
                || lower.contains("aarch64") || lower.contains("arm64")) {
            return CpuArchitecture.ARM_64;
        }
        // If you want to detect 32-bit ARM:
        if (lower.contains("armv7") || lower.contains("armv8") || lower.contains("cortex"))
            return CpuArchitecture.ARM;
        // Optionally handle other known architectures
        if (lower.contains("riscv")) {
            return CpuArchitecture.RISC;
        }
        if (lower.contains("power") || lower.contains("ppc")) {
            return CpuArchitecture.PPC;
        }
        return CpuArchitecture.UNKNOWN;
    }

    /**
     * Returns a list of Memory modules installed in the computer.
     * The method does NOT assume that all modules will be alike.
     *
     * @return List of {@link MemoryModule} objects
     */
    public Set<MemoryModule> detectMemoryModules() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        List<PhysicalMemory> modules = hardware.getMemory().getPhysicalMemory();
        Set<MemoryModule> memoryModules = new HashSet<>();
        for (PhysicalMemory module : modules) {
            MemoryModule memoryModule = MemoryModule.builder()
                    .busSpeed((int) (module.getClockSpeed() / 1_000_000)) // Convert to MHz
                    .capacity((int) (module.getCapacity() / (1024 * 1024))) // Convert to MB
                    .type(MemoryType.UNKNOWN)
                    .build();
            memoryModule.setManufacturer(module.getManufacturer());
            memoryModule.setType(identifyMemoryType(module.getMemoryType()));
            memoryModule.setName(module.getManufacturer() + " " + memoryModule.getCapacity() + "MB " + module.getMemoryType());
            memoryModule.setModel(module.getPartNumber());
            memoryModule.setProductIdentifier(module.getSerialNumber());
            memoryModule.setDescription(module.toString());
            memoryModules.add(memoryModule);
        }
        return memoryModules;
    }

    /**
     * Returns the Type of memory by string. The method tries to match substring of memoryType parameter.
     * Usable for both CPU and GPU memories
     *
     * @param memoryType should be fully qualified name of CPU
     * @return MemoryType enum
     */
    private MemoryType identifyMemoryType(String memoryType) {
        String upper = memoryType.toUpperCase();
        if (upper.contains("DDR2")) {
            return MemoryType.DDR2;
        } else if (upper.contains("DDR3")) {
            return MemoryType.DDR3;
        } else if (upper.contains("DDR4")) {
            return MemoryType.DDR4;
        } else if  (upper.contains("DDR5")) {
            return MemoryType.DDR5;
        } else if (upper.contains("GDDR5X")) {
            return MemoryType.GDDR5X;
        } else if (upper.contains("GDDR5")) {
            return MemoryType.GDDR5;
        } else if (upper.contains("GDDR6X")) {
            return MemoryType.GDDR6X;
        } else if (upper.contains("GDDR6")) {
            return MemoryType.GDDR6;
        }
        return MemoryType.UNKNOWN;
    }

    /**
     * Returns a list of GPUs installed in the computer. Detects both integrate and discrete GPUs.
     * Caution! Several attributes are being retrieved using AIService. 100% accuracy is not guaranteed
     *
     * @return List of {@link Gpu} objects
     */
    public Set<Gpu> detectGpus() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        List<GraphicsCard> graphicsCards = hardware.getGraphicsCards();
        Set<Gpu> gpus = new HashSet<>();
        for (GraphicsCard graphicsCard : graphicsCards) {
            Gpu gpu = Gpu.builder()
                    .capacity((int)(graphicsCard.getVRam() / (1024 * 1024))) // Convert to MB
                    .type(MemoryType.UNKNOWN)
                    .build();
            gpu.setName(graphicsCard.getName());
            gpu.setManufacturer(graphicsCard.getVendor());
            gpu.setProductIdentifier(graphicsCard.getDeviceId());
            gpu.setDescription(graphicsCard.toString());

            // Fetch missing specifications from AIService for each GPU
            try {
                String context = "You are an encyclopedia of computer hardware. Your responses are always a valid JSON Object.";
                String prompt = "Provide specifications of GPU:" + gpu.getManufacturer() + "; " + gpu.getName() + ". " +
                        "Do not convert value units, like Kilo, Mega or Giga. Keep them as-is." +
                        "MemoryType should be one of these values: " +
                        Arrays.toString(MemoryType.values()) +
                        "; " +
                        "Architecture should be one of these values: " +
                        Arrays.toString(GpuArchitecture.values()) +
                        "; " +
                        "Aliases for Shader cores are Parallel cores, Streaming cores, CUDA cores; " +
                        "Model should only mention the series and model, not the vendor, e.g. Geforce RTX 4070 Ti; Radeon RX 7900 XTX";
                Map<String, String> schemaMap = new HashMap<>();
                schemaMap.put("model", "string");
                schemaMap.put("architecture", "string");
                schemaMap.put("idDiscontinued", "boolean");
                schemaMap.put("yearReleased", "integer");
                schemaMap.put("tdp", "integer");
                schemaMap.put("minClock", "integer");
                schemaMap.put("maxClock", "integer");
                schemaMap.put("memoryType", "string");
                schemaMap.put("shaderCores", "integer");
                schemaMap.put("tensorCores", "integer");

                Map<String, Object> response = httpUtil.getStructuredAiResponse(BASE_URL, context, prompt, schemaMap);
                gpu.setModel((String)response.get("model"));
                gpu.setArchitecture(identifGpuArchitecture((String)response.get("architecture")));
                gpu.setIsDiscontinued((boolean)response.get("isDiscontinued"));
                gpu.setYearReleased((int)response.get("yearReleased"));
                gpu.setTdpWatts((int)response.get("tdp"));
                gpu.setMinClock((int)response.get("minClock"));
                gpu.setMaxClock((int)response.get("maxClock"));
                gpu.setShaderCores((int)response.get("shaderCores"));
                gpu.setTensorCores((int)response.get("tensorCores"));
                gpu.setType(MemoryType.valueOf((String)response.get("memoryType")));
            } catch (Exception e) {
                log.error("Failed to generate response", e);
            }
            gpus.add(gpu);
        }
        return gpus;
    }

    /**
     * Returns the architecture of GPU by string name. This method matches exact string with the enumerations.
     * If no value is matched, then UNKNOWN is returned
     *
     * @param architecture as string
     * @return GpuArchitecture enum
     */
    private GpuArchitecture identifGpuArchitecture(String architecture) {
        // Try to convert directly into Enum
        try {
            return GpuArchitecture.valueOf(architecture.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GpuArchitecture.UNKNOWN;
        }
    }

    /**
     * Returns a list of Storage devices installed in the computer. It does NOT detect partitions, only the physical storages
     * Caution! Several attributes are being retrieved using AIService. 100% accuracy is not guaranteed
     *
     * @return List of {@link Storage} objects
     */
    public Set<Storage> detectStorages() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        List<HWDiskStore> diskStores = hardware.getDiskStores();
        Set<Storage> storages = new HashSet<>();
        for (HWDiskStore diskStore : diskStores) {
            Storage storage = Storage.builder()
                    .type(StorageType.EXPRESS_SOLID_STATE)
                    .capacity((int) (diskStore.getSize() / (1024 * 1024 *  1024))) // Convert to GB
                    .cache(0)
                    .build();
            storage.setName(diskStore.getModel());
            storage.setModel(diskStore.getModel());
            storage.setProductIdentifier(diskStore.getSerial());
            storage.setDescription(diskStore.toString());
            storage.setManufacturer("");
            storage.setIsDiscontinued(false);

            // Fetch missing specifications from AIService
            try {
                String context = "You are an encyclopedia of computer hardware. Your responses are always a valid JSON Object.";
                String prompt = "Provide specifications of Hard disk/SSD model:" +
                        diskStore.getModel() +
                        ". " +
                        "Do not convert value units, like Kilo, Mega or Giga. Keep them as-is." +
                        "StorageType should be one of these values: " +
                        Arrays.toString(StorageType.values()) +
                        "; " +
                        "CacheInBytes applies if HDD has a cache memory or an SSD has NAND cache memory. Return 0 there is no cache";
                Map<String, String> schemaMap = new HashMap<>();
                schemaMap.put("manufacturer", "string");
                schemaMap.put("idDiscontinued", "boolean");
                schemaMap.put("tdp", "integer");
                schemaMap.put("yearReleased", "integer");
                schemaMap.put("cacheSize", "integer");

                Map<String, Object> response = httpUtil.getStructuredAiResponse(BASE_URL, context, prompt, schemaMap);
                storage.setManufacturer((String) response.get("manufacturer"));
                storage.setIsDiscontinued((boolean)response.get("idDiscontinued"));
                storage.setYearReleased((int)response.get("yearReleased"));
                storage.setTdpWatts((int)response.get("tdp"));
                storage.setCache(((int)response.get("cacheSize")) / (1024 * 1024));
            } catch (Exception e) {
                log.error("Failed to generate response", e);
            }
            storages.add(storage);
        }
        return storages;
    }

    /**
     * Returns a list of Network Interface Controlling devices (both Physical and Virtual).
     * Caution! Several attributes are being retrieved using AIService. 100% accuracy is not guaranteed
     *
     * @return List of {@link NetworkDevice} objects
     */
    public Set<NetworkDevice> detectNetworkInterfaces() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        Set<NetworkDevice> nics = new HashSet<>();
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
            NetworkDevice nic = NetworkDevice.builder()
                    .macAddress(networkIF.getMacaddr())
                    .speed((int) (networkIF.getSpeed() / (1_000_000)))
                    .build();
            nic.setManufacturer(getVendorByMacAddress(nic.getMacAddress()));
            nic.setName(networkIF.getDisplayName());
            nics.add(nic);
        }
        return nics;
    }

    /**
     * Detects various sensors that may be installed in the computer and returns a Map of Names and booleans.
     * Value True represents that the sensor exists in the computer, not its readings.
     * See "Read" methods in {@link HardwareUtil} class
     *
     * @return Map of Sensor names and respective Boolean values
     */
    public Map<String, Boolean> detectSensors() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        Sensors sensors = hardware.getSensors();
        Map<String, Boolean> sensorsMap = new HashMap<>();
        sensorsMap.put("cpu_thermometer", sensors.getCpuTemperature() != 0);
        sensorsMap.put("cpu_voltmeter", sensors.getCpuVoltage() != 0);
        sensorsMap.put("cpu_fans", sensors.getFanSpeeds().length > 0);
        return sensorsMap;
    }

    /**
     * Returns a list of power sources, mainly batteries in the computer.
     * Note! Due to architectural limitations, specifications of external Power Units is mainly undetectable
     *
     * @return List of {@link GenericComponent} objects
     */
    public Set<GenericComponent> detectPowerSources() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        Set<GenericComponent> genericComponents = new HashSet<>();

        List<PowerSource> powerSources = hardware.getPowerSources();
        powerSources.forEach(powerSource -> {
            // Desktop computers have Power Supply Units that cannot be detected on OS-level.
            boolean isExternalPSU = powerSource.isPowerOnLine() && powerSource.getVoltage() >= 0;
            GenericComponent power = GenericComponent.builder()
                    .specificationKey("capacity")
                    .specificationValue(isExternalPSU ? "0" : String.valueOf(powerSource.getMaxCapacity()))
                    .build();
            power.setType(DeviceType.POWER_SOURCE);
            power.setIsDiscontinued(false);
            if (isExternalPSU) {
                power.setName(powerSource.getDeviceName());
                power.setManufacturer(powerSource.getManufacturer());
                power.setTdpWatts(powerSource.getDesignCapacity());
                power.setYearReleased(powerSource.getManufactureDate() == null ? null : powerSource.getManufactureDate().getYear());
                power.setProductIdentifier(powerSource.getSerialNumber());
            }
            power.setSpecificationKey("is_external_power");
            power.setSpecificationValue(String.valueOf(isExternalPSU));
            power.setDescription(powerSource.toString());
            genericComponents.add(power);
        });
        return genericComponents;
    }

    /**
     * Returns a list of USB devices connected to the computer.
     * This method detects all USB devices including hubs, storage devices, and peripherals.
     *
     * @return List of {@link GenericComponent} objects representing USB devices.
     */
    public Set<GenericComponent> detectUsbDevices() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        Set<GenericComponent> genericComponents = new HashSet<>();

        // Get all USB devices connected to the system
        List<UsbDevice> usbDevices = hardware.getUsbDevices(false); // 'false' excludes nested devices for cleaner output

        for (UsbDevice usbDevice : usbDevices) {
            GenericComponent usb = GenericComponent.builder()
                    .build();
            usb.setType(DeviceType.PERIPHERAL);
            usb.setName(usbDevice.getName());
            usb.setModel(usbDevice.getProductId());
            usb.setManufacturer(usbDevice.getVendor().isEmpty() ? usbDevice.getVendorId() : usbDevice.getVendor());
            usb.setProductIdentifier(usbDevice.getUniqueDeviceId());
            usb.setDescription(usbDevice.toString());

            // Fetch missing specifications from AIService
            try {
                String context = "You are an encyclopedia of computer hardware. Your responses are always a valid JSON Object.";
                String prompt = "Provide specifications of USB Device: "
                        + usbDevice.getName() + ". "
                        + "Do not convert value units, like Kilo, Mega or Giga. Keep them as-is. "
                        + "If TDP (thermal design power) is unavailable, then put in an estimate."
                        + "Type should be one of these values: "
                        + Arrays.toString(DeviceType.values()) + "; "
                        + "Purpose should be a short string which describes primary use of the device";

                // Define the expected schema for AIService
                Map<String, String> schemaMap = new HashMap<>();
                schemaMap.put("model", "string");
                schemaMap.put("idDiscontinued", "boolean");
                schemaMap.put("yearReleased", "integer");
                schemaMap.put("tdp", "integer");
                schemaMap.put("type", "string");
                schemaMap.put("purpose", "string");

                Map<String, Object> response = httpUtil.getStructuredAiResponse(BASE_URL, context, prompt, schemaMap);
                usb.setModel(response.getOrDefault("model", usbDevice.getProductId()).toString());
                usb.setIsDiscontinued((boolean)response.getOrDefault("idDiscontinued", false));
                usb.setYearReleased((int)response.getOrDefault("yearReleased", "1900"));
                usb.setTdpWatts(response.containsKey("tdp") ? (int) response.get("tdp") : 0);
                usb.setType(DeviceType.valueOf((String) response.get("type")));

                // Assign the "purpose" to specificationKey / specificationValue
                usb.setSpecificationKey("purpose");
                usb.setSpecificationValue(response.containsKey("purpose") ? response.get("purpose").toString() : "N/A");
            } catch (Exception e) {
                log.error("Failed to generate response", e);
            }
            genericComponents.add(usb);
        }
        return genericComponents;
    }

    /**
     * Returns the current memory usage as a percentage (0-100).
     */
    public double readCurrentMemoryUsage() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        double memoryUsage = ((double) usedMemory / totalMemory) * 100.0;
        return Math.round(memoryUsage * 100.0) / 100.0;
    }

    /**
     * Returns the current CPU usage as a percentage (0-100) across all cores.
     */
    public double readCurrentCpuUsage() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        CentralProcessor processor = hardware.getProcessor();
        // Use cached ticks from last call to compute a more stable usage window
        long[] previous = lastCpuTicks;
        long[] current = processor.getSystemCpuLoadTicks();
        lastCpuTicks = current;

        // On the very first call there is no previous baseline; return 0 and let the next call compute
        if (previous == null) {
            return 0.0;
        }

        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(previous) * 100.0;
        return Math.min(100.0, Math.max(0.0, Math.round(cpuUsage * 100.0) / 100.0));
    }

    /**
     * Returns last reading from CPU's thermal sensor
     */
    public double readCurrentCpuTemperature() {
//        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
//        return hardware.getSensors().getCpuTemperature();
        return 0f;
    }

    /**
     * Returns last reading from CPU's voltage sensor
     */
    public double readCurrentCpuVoltage() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        return hardware.getSensors().getCpuVoltage();
    }

    /**
     * Returns last readings from all Fans in an array
     */
    public int[] readCurrentFanSpeeds() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        return hardware.getSensors().getFanSpeeds();
    }

    /**
     * Executes the nvidia-smi command to get GPU metrics and returns the parsed output.
     * The command queries GPU utilization, temperature, power draw, and memory usage.
     *
     * @return A map containing the parsed metrics, or null if the command fails
     */
    private Map<String, String> readGpuUsage() {
        HardwareAbstractionLayer hardware = detectHardwareSpecifications();
        if (hardware.getGraphicsCards().isEmpty()) {
            return null;
        }

        try {
            String command = "nvidia-smi --query-gpu=utilization.gpu,temperature.gpu,power.draw,memory.total,memory.used --format=csv";
            String output = OSUtil.executeCommand(command);

            if (output == null || output.trim().isEmpty()) {
                // Try absolute path for Windows as fallback
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    String fallbackPath = "C:\\Windows\\System32\\nvidia-smi.exe";
                    output = OSUtil.executeCommand(fallbackPath + " --query-gpu=utilization.gpu,temperature.gpu,power.draw,memory.total,memory.used --format=csv");
                }
            }

            if (output == null || output.trim().isEmpty()) {
                log.warn("Failed to execute nvidia-smi command or empty output");
                return null;
            }

            // Parse the CSV output
            String[] lines = output.trim().split("\\n");
            if (lines.length < 2) {
                log.warn("Invalid nvidia-smi output format: {}", output);
                return null;
            }

            // Get the data line (skip header)
            String dataLine = lines[1].trim();
            String[] values = dataLine.split(",");

            if (values.length < 5) {
                log.warn("Invalid nvidia-smi data format: {}", dataLine);
                return null;
            }

            Map<String, String> result = new HashMap<>();
            result.put("gpuUsage", values[0].trim().replace("%", "").trim());
            result.put("gpuTemp", values[1].trim());
            result.put("gpuPower", values[2].trim().replace("W", "").trim());
            result.put("gpuMemTotal", values[3].trim().replace("MiB", "").trim());
            result.put("gpuMemUsed", values[4].trim().replace("MiB", "").trim());

            return result;
        } catch (Exception e) {
            log.error("Error executing nvidia-smi command", e);
            return null;
        }
    }

    /**
     * Returns the current GPU usage as a percentage (0-100).
     */
    public double readCurrentGpuUsage() {
        Map<String, String> metrics = readGpuUsage();
        if (metrics == null || !metrics.containsKey("gpuUsage")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(metrics.get("gpuUsage"));
        } catch (NumberFormatException e) {
            log.error("Error parsing GPU usage", e);
            return 0.0;
        }
    }

    /**
     * Returns the current GPU temperature.
     */
    public double readCurrentGpuTemperature() {
        Map<String, String> metrics = readGpuUsage();
        if (metrics == null || !metrics.containsKey("gpuTemp")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(metrics.get("gpuTemp"));
        } catch (NumberFormatException e) {
            log.error("Error parsing GPU temperature", e);
            return 0.0;
        }
    }

    /**
     * Returns the current GPU memory usage as a percentage (0-100).
     */
    public double readCurrentGpuMemoryUsage() {
        Map<String, String> metrics = readGpuUsage();
        if (metrics == null || !metrics.containsKey("gpuMemTotal") || !metrics.containsKey("gpuMemUsed")) {
            return 0.0;
        }

        try {
            double total = Double.parseDouble(metrics.get("gpuMemTotal"));
            double used = Double.parseDouble(metrics.get("gpuMemUsed"));

            if (total <= 0) {
                return 0.0;
            }

            double percentage = (used / total) * 100.0;
            return Math.round(percentage * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            log.error("Error parsing GPU memory usage", e);
            return 0.0;
        }
    }

    /**
     * Returns the current GPU memory usage as a string (used/total in MiB).
     */
    public String readCurrentGpuMemoryInfo() {
        Map<String, String> metrics = readGpuUsage();
        if (metrics == null || !metrics.containsKey("gpuMemTotal") || !metrics.containsKey("gpuMemUsed")) {
            return "0/0 MiB";
        }

        return metrics.get("gpuMemUsed") + "/" + metrics.get("gpuMemTotal") + " MiB";
    }

    /**
     * Returns the current GPU power consumption in watts.
     */
    public double readCurrentGpuPowerConsumption() {
        Map<String, String> metrics = readGpuUsage();
        if (metrics == null || !metrics.containsKey("gpuPower")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(metrics.get("gpuPower"));
        } catch (NumberFormatException e) {
            log.error("Error parsing GPU power consumption", e);
            return 0.0;
        }
    }

    /**
     * Detects the underlying Operating System running on the computer
     */
    public String detectOperatingSystem() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();
        String osFamily = os.getFamily();
        String version = os.getVersionInfo().getVersion();

        return osFamily + " " + version;
    }

    /**
     * Detects and creates a comprehensive Computer object by aggregating all hardware components.
     * This method performs a full system scan to detect and collect information about:
     * - CPUs
     * - Memory modules
     * - GPUs
     * - Operating Systems
     * - Storage devices
     * - Power sources
     * - Network interfaces
     * - USB devices
     *
     * @return {@link Computer} object containing all detected hardware components
     */

    public Computer detectComputer() {
        Set<Cpu> cpus = detectCpus();

        Set<MemoryModule> memoryModules = detectMemoryModules();

        Set<Gpu> gpus = detectGpus();

        Set<Storage> storages = detectStorages();

        String operatingSystem = detectOperatingSystem();

        Set<GenericComponent> powerSources = detectPowerSources();

        Set<NetworkDevice> networks = detectNetworkInterfaces();

        Set<GenericComponent> usbDevices = detectUsbDevices();

        Computer computer = new Computer();
        computer.setCpus(cpus);
        computer.setGpus(gpus);
        //computer.setName(); TODO
        //computer.setComputerAttributes(); // TODO
        //computer.setType(); // TODO
        computer.setMemories(memoryModules);
        computer.setStorages(storages);
        computer.setNetworkDevices(networks);
        computer.setOperatingSystem(operatingSystem);
        computer.addOtherComponents(powerSources);
        computer.addOtherComponents(usbDevices);

        return computer;
    }

    /**
     * Deep cleans the free space in given directory, so that it's difficult to run data recovery attacks.
     * The method is asynchronous and uses isDeepCleaning flag to make sure that no two instances are run simultaneously
     *
     * @param directoryPath to deep clean free space from
     * @return CompletableFuture<Boolean> indicating success or failure of job
     */
    public CompletableFuture<Boolean> deepCleanFreeSpace(String directoryPath) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isDeepCleaning.compareAndSet(false, true)) {
                log.info("Deep cleaning is in progress");
                return false;
            }
            try {
                //TODO: Use OSUtil to execute commands to deep clean the free space in given directory path
                // REMEMBER to check for the OS. Use an external library if that's convenient
                Thread.sleep(1000);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } finally {
                isDeepCleaning.set(false);
            }
        }, executorService);
    }
}
