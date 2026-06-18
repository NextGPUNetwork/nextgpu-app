package ai.nextgpu.agent.service;

import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.ComputerDto;
import ai.nextgpu.common.dto.CpuDto;
import ai.nextgpu.common.dto.CreateComputerDto;
import ai.nextgpu.common.dto.GenericComponentDto;
import ai.nextgpu.common.dto.GpuDto;
import ai.nextgpu.common.dto.MemoryModuleDto;
import ai.nextgpu.common.dto.NetworkDeviceDto;
import ai.nextgpu.common.dto.StorageDto;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.report.HardwareReport;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComputerService {

    private static final Logger log = LoggerFactory.getLogger(ComputerService.class);

    @Value("${nextgpu.web.baseUrl:http://localhost:8080}")
    private String BASE_URL;

    private final DataService dataService;
    private final HardwareUtil hardwareUtil;
    private final BenchmarkUtil benchmarkUtil;
    private final HttpUtil httpUtil;
    private final NextGpuWebService nextGpuWebService;

    @Autowired
    public ComputerService(
            DataService dataService,
            @Lazy HardwareUtil hardwareUtil,
            @Lazy BenchmarkUtil benchmarkUtil,
            HttpUtil httpUtil,
            NextGpuWebService nextGpuWebService) {
        this.dataService = dataService;
        this.hardwareUtil = hardwareUtil;
        this.benchmarkUtil = benchmarkUtil;
        this.httpUtil = httpUtil;
        this.nextGpuWebService = nextGpuWebService;
    }

    /**
     * Generates a computer benchmark report by executing various benchmark tests
     * and saves the results into the persistent data store.
     *
     * @param loginWallet The wallet address of the provider associated with the computer to benchmark.
     * @param quick A boolean flag indicating whether to perform a quick benchmark (true) or a detailed benchmark (false).
     * @return A {@code BenchmarkReport} instance containing the results of the benchmark tests.
     * @throws Exception If any errors occur during the benchmarking process or data persistence.
     */
    public BenchmarkReport generateComputerBenchmarkReport(String loginWallet, boolean quick) throws Exception {
        long startTime = System.currentTimeMillis();

        Map<String, Object> gpu = benchmarkUtil.benchmarkGpu();
        Map<String, Object> memory = benchmarkUtil.benchmarkMemory(quick);
        Map<String, Object> storage = benchmarkUtil.benchmarkStorage(quick);
        Map<String, Object> cpu = benchmarkUtil.benchmarkCpu();
        Map<String, Object> network = null; //TODO
        Map<String, Object> other = null; //TODO

        BenchmarkReport benchmarkReport = new BenchmarkReport();
        benchmarkReport.setCpuBenchmarkResults(cpu);
        benchmarkReport.setGpuBenchmarkResults(gpu);
        benchmarkReport.setMemoryBenchmarkResults(memory);
        benchmarkReport.setStorageBenchmarkResults(storage);
        benchmarkReport.setNetworkBenchmarkResults(network);
        benchmarkReport.setOtherBenchmarkResults(other);
        benchmarkReport.setProvider(dataService.findProviderByWalletAddress(loginWallet));

        Computer computer = dataService.findAllComputers().stream()
                .filter(c -> c.getProvider()
                        .getWalletAddress()
                        .equalsIgnoreCase(loginWallet)).findFirst().get();
        benchmarkReport.setComputer(computer);
        benchmarkReport.setElapsedTime(System.currentTimeMillis() - startTime);

        return dataService.saveBenchmarkReport(benchmarkReport);
    }

    public Map<String, Object> generateComputerUsageReport() {
        Map<String, Object> usage = new HashMap<>();
        usage.put("cpuTemperature", hardwareUtil.readCurrentCpuTemperature());
        usage.put("cpuUsage", hardwareUtil.readCurrentCpuUsage());
        usage.put("cpuVoltage", hardwareUtil.readCurrentCpuVoltage());
        usage.put("cpuFanSpeed", hardwareUtil.readCurrentFanSpeeds());
        usage.put("memoryUsage", hardwareUtil.readCurrentMemoryUsage());
        usage.put("gpuMemoryUsage", hardwareUtil.readCurrentGpuMemoryUsage());
        usage.put("gpuMemoryInfo", hardwareUtil.readCurrentGpuMemoryInfo());
        usage.put("gpuPowerConsumption", hardwareUtil.readCurrentGpuPowerConsumption());
        usage.put("gpuTemperature", hardwareUtil.readCurrentGpuTemperature());
        usage.put("gpuUsage", hardwareUtil.readCurrentGpuUsage());
        return usage;
    }

    /**
     * Saves the detected hardware information and associates it with a user's account.
     * <p>
     * This method attempts to match detected hardware components with existing database entries
     * to avoid duplication. If no matching entry is found, the component is saved as a new entity.
     * The method also communicates with an external service to create or update the computer.
     *
     * @param detectedHardware The hardware information detected on the user's system,
     *                         including CPUs, GPUs, memories, storages, network devices,
     *                         and generic components.
     * @param loginWallet      The unique identifier for the user's account to associate the computer with.
     * @return The saved {@link Computer} entity with complete associations of components and attributes.
     * @throws RuntimeException If an error occurs during hardware detection or saving the computer.
     */
    @Transactional
    public Computer saveComputer(Computer detectedHardware, String loginWallet) {
        Computer localComputerEntity = loadOrCreateSingleLocalComputer(loginWallet);

        CreateComputerDto createComputerDto = buildCreateComputerDto(loginWallet, localComputerEntity, detectedHardware);

        try {
            ComputerDto response = nextGpuWebService.createComputer(createComputerDto);

            // Resolve detected components to existing DB entities (dedupe) BEFORE attaching to the Computer.
            Set<Cpu> resolvedCpus = resolveComponents(detectedHardware.getCpus(), dataService::saveCpu, dataService.getCpuRepository());
            Set<Gpu> resolvedGpus = resolveComponents(detectedHardware.getGpus(), dataService::saveGpu, dataService.getGpuRepository());
            Set<MemoryModule> resolvedMemories = resolveComponents(detectedHardware.getMemories(), dataService::saveMemoryModule, dataService.getMemoryModuleRepository());
            Set<Storage> resolvedStorages = resolveComponents(detectedHardware.getStorages(), dataService::saveStorage, dataService.getStorageRepository());
            Set<NetworkDevice> resolvedNetworkDevices = resolveComponents(detectedHardware.getNetworkDevices(), dataService::saveNetworkDevice, dataService.getNetworkDeviceRepository());

            Set<GenericComponent> resolvedGenericComponents = resolveGenericComponents(detectedHardware.getOtherComponents());
            Map<ComputerAttributeType, String> resolvedComputerAttributes = resolveComputerAttributes(detectedHardware.getComputerAttributes());

            BeanUtils.copyProperties(detectedHardware, localComputerEntity, "id", "uuid", "provider", "cpus", "gpus", "memories", "storages", "networkDevices", "otherComponents", "computerAttributes");
            localComputerEntity.setCpus(resolvedCpus);
            localComputerEntity.setGpus(resolvedGpus);
            localComputerEntity.setMemories(resolvedMemories);
            localComputerEntity.setStorages(resolvedStorages);
            localComputerEntity.setNetworkDevices(resolvedNetworkDevices);
            localComputerEntity.setOtherComponents(resolvedGenericComponents);
            localComputerEntity.setComputerAttributes(resolvedComputerAttributes);

            // The UUID of the local computer entity must be the same as the one returned by the API
            localComputerEntity.setUuid(response.getUuid());

            return dataService.saveComputer(localComputerEntity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to detect and save hardware", e);
        }
    }

    private <T extends BaseComponent, D extends BaseComponent> Set<D> resolveComponents(
            Set<T> detected,
            Function<T, D> saveComponent,
            BaseComponentRepository<T, Long> repository) {
        if (detected == null || detected.isEmpty()) {
            return Collections.emptySet();
        }

        // Deduplicate within this run by natural key first.
        Map<String, T> uniqueByKey = new LinkedHashMap<>();
        for (T component : detected) {
            String key = naturalKey(component.getManufacturer(), component.getModel());
            uniqueByKey.putIfAbsent(key, component);
        }

        return uniqueByKey.values().stream()
                .map(component -> {
                    Optional<T> existing = repository
                            .findByManufacturerAndModel(component.getManufacturer(), component.getModel());

                    if(existing.isPresent()){
                        T managed = existing.get();
                        BeanUtils.copyProperties(component, managed, "id", "uuid", "computers");
                        return saveComponent.apply(managed);
                    } else {
                        // First time seen: create once.
                        return saveComponent.apply(component);
                    }
                }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String naturalKey(String manufacturer, String model) {
        String m1 = (manufacturer == null) ? "" : manufacturer.trim().toLowerCase();
        String m2 = (model == null) ? "" : model.trim().toLowerCase();
        return m1 + "||" + m2;
    }

    private Map<ComputerAttributeType, String> resolveComputerAttributes(
            Map<ComputerAttributeType, String> detected) {

        if (detected == null || detected.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ComputerAttributeType, String> resolved = new LinkedHashMap<>();

        for (Map.Entry<ComputerAttributeType, String> e : detected.entrySet()) {
            ComputerAttributeType key = e.getKey();
            String value = e.getValue();

            if (key == null) continue;

            Optional<ComputerAttributeType> canonical =
                    hasText(key.getUuid())
                            ? dataService.getComputerAttributeTypeRepository().findByUuid(key.getUuid())
                            : hasText(key.getName())
                            ? dataService.getComputerAttributeTypeRepository().findByNameIgnoreCase(key.getName())
                            : Optional.empty();

            if (canonical.isEmpty()) {
                // Choose behavior:
                // - skip unknown keys (lenient)
                // - or throw (strict)
                continue;
            }

            resolved.put(canonical.get(), value);
        }

        return resolved;
    }

    /**
     * Enforces the "only one Computer entity locally" rule and returns it,
     * or creates a new one bound to the current provider.
     */
    private Computer loadOrCreateSingleLocalComputer(String loginWallet) {
        List<Computer> computers = dataService.findAllComputers().stream()
                .filter(c -> c.getProvider()
                        .getWalletAddress()
                        .equalsIgnoreCase(loginWallet)
                )
                .toList();
        if (computers.size() > 1) {
            throw new RuntimeException("More than one Computer entity detected in the database for wallet address: " + loginWallet);
        }

        if (!computers.isEmpty()) {
            return computers.getFirst();
        }

        Provider provider = dataService.findProviderByWalletAddress(loginWallet);
        Computer computer = new Computer();
        computer.setProvider(provider);
        computer.setUuid(null);
        return computer;
    }

    /**
     * Builds the payload sent to the remote API from the detected hardware plus identity fields.
     * Note about UUID semantics:
     * - If localComputerEntity has a UUID, we send it (treat as "update/upsert" depending on API).
     * - Otherwise, we send null (treat as "create").
     */
    private CreateComputerDto buildCreateComputerDto(String loginWallet, Computer localComputerEntity, Computer detectedHardware) {
        CreateComputerDto dto = new CreateComputerDto();
        dto.setWalletAddress(loginWallet);
        dto.setUuid(localComputerEntity.getUuid()); // null on the first creation, non-null on subsequent syncs

        dto.setType(detectedHardware.getType());
        dto.setOperatingSystem(detectedHardware.getOperatingSystem());
        dto.setCpus(detectedHardware.getCpus().stream().map(CpuDto::toDto).toList());
        dto.setGpus(detectedHardware.getGpus().stream().map(GpuDto::toDto).toList());
        dto.setMemories(detectedHardware.getMemories().stream().map(MemoryModuleDto::toDto).toList());
        dto.setStorages(detectedHardware.getStorages().stream().map(StorageDto::toDto).toList());
        dto.setNetworkDevices(detectedHardware.getNetworkDevices().stream().map(NetworkDeviceDto::toDto).toList());
        dto.setOtherComponents(detectedHardware.getOtherComponents().stream().map(GenericComponentDto::toDto).toList());
        dto.setComputerAttributes(ComputerDto.toComputerAttributeDtosMap(detectedHardware.getComputerAttributes()));

        return dto;
    }

    private Set<GenericComponent> resolveGenericComponents(Set<GenericComponent> detected) {
        if (detected == null || detected.isEmpty()) return Collections.emptySet();

        Map<String, GenericComponent> uniqueByKey = new LinkedHashMap<>();
        for (GenericComponent gc : detected) {
            uniqueByKey.putIfAbsent(genericComponentKey(gc), gc);
        }

        return uniqueByKey.values().stream()
                .map(gc -> {
                    Optional<GenericComponent> existing;

                    if (hasText(gc.getProductIdentifier())) {
                        existing = dataService.getGenericComponentRepository()
                                .findByTypeAndProductIdentifier(gc.getType(), gc.getProductIdentifier());
                    } else {
                        existing = dataService.getGenericComponentRepository()
                                .findByFingerprint(gc.getType(), gc.getManufacturer(), gc.getModel(), gc.getName(),
                                        gc.getSpecificationKey(), gc.getSpecificationValue());
                    }

                    if (existing.isPresent()) {
                        GenericComponent managed = existing.get();
                        // full refresh of mutable fields; keep identity fields
                        BeanUtils.copyProperties(gc, managed, "id", "uuid", "computers");
                        return dataService.saveGenericComponent(managed);
                    }

                    return dataService.saveGenericComponent(gc);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String genericComponentKey(GenericComponent gc) {
        // Prefer productIdentifier if present
        if (hasText(gc.getProductIdentifier())) {
            return "pid||" + norm(gc.getType()) + "||" + norm(gc.getProductIdentifier());
        }
        // Otherwise fingerprint
        return "fp||" + norm(gc.getType()) + "||" + norm(gc.getManufacturer()) + "||" + norm(gc.getModel()) + "||"
                + norm(gc.getName()) + "||" + norm(gc.getSpecificationKey()) + "||" + norm(gc.getSpecificationValue());
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String norm(Object o) {
        if (o == null) return "";
        return o.toString().trim().toLowerCase().replaceAll("\\s+", " ");
    }


    /**
     * Generates a hardware report for the computer. The report includes details about CPUs,
     * GPUs, memory modules, storage, network devices, power sources, and USB devices. The report
     * can be exported in either text format or HTML format based on the provided flag.
     * This method handles the saving of the Computer entity and all its components in a graceful
     * and dynamic way. It ensures that the Computer entity is saved with all its components before
     * saving the HardwareReport.
     *
     * @param asText a boolean flag indicating if the report should be exported as a text file.
     *               If true, the report is exported as a .txt file; otherwise, it is exported as an .html file.
     * @return the generated HardwareReport object containing detailed hardware information and export metadata.
     */
    public HardwareReport generateComputerHardwareReport(String loginWallet, boolean asText) throws Exception {
        long startTime = System.currentTimeMillis();

        // Detect all hardware components
        Set<Cpu> cpus = hardwareUtil.detectCpus();
        Set<Gpu> gpus = hardwareUtil.detectGpus();
        Set<MemoryModule> memories = hardwareUtil.detectMemoryModules();
        Set<Storage> storages = hardwareUtil.detectStorages();
        Set<NetworkDevice> networkDevices = hardwareUtil.detectNetworkInterfaces();
        Set<GenericComponent> powerSources = hardwareUtil.detectPowerSources();
        Set<GenericComponent> usbDevices = hardwareUtil.detectUsbDevices();

        // Update computer with detected components
        Computer computer = Computer.builder()
                .cpus(cpus)
                .gpus(gpus)
                .memories(memories)
                .storages(storages)
                .networkDevices(networkDevices)
                .build();
        computer.setName(loginWallet);
        computer.setOtherComponents(powerSources);
        computer.getOtherComponents().addAll(usbDevices);

        // Create and configure a hardware report
        HardwareReport report = new HardwareReport();
        report.setElapsedTime(System.currentTimeMillis() - startTime);

        // Save computer with all its components in a single transaction
        //TODO: Enable after the functionality to search components by matching is complete
        // computer = dataService.saveComputer(computer);
        report.setComputer(computer);

        // Save and return the report
        HardwareReport hardwareReport = report; //TODO: enable after the previous TODO is done dataService.saveHardwareReport(report);
        return hardwareReport;
    }
}
