package ai.nextgpu.agent.service;

import ai.nextgpu.agent.repository.*;
import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.ComputerAttributeTypeDto;
import ai.nextgpu.common.dto.ComputerDto;
import ai.nextgpu.common.dto.CreateComputerDto;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.report.HardwareReport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ComputerServiceTest {
    @Mock private DataService dataService;
    @Mock private HardwareUtil hardwareUtil;
    @Mock private BenchmarkUtil benchmarkUtil;
    @Mock private HttpUtil httpUtil;
    @Mock private NextGpuWebService nextGpuWebService;
    @Mock private ComputerAttributeTypeRepository computerAttributeTypeRepository;
    @Mock private ComputerRepository computerRepository;
    @Mock private GlobalPropertyRepository globalPropertyRepository;
    private ComputerService computerService;
    private final String loginWallet = "wallet-123";

    @BeforeEach
    void setUp() {
        computerService = new ComputerService(
                dataService,
                hardwareUtil,
                benchmarkUtil,
                httpUtil,
                nextGpuWebService,
                computerAttributeTypeRepository,
                computerRepository,
                globalPropertyRepository
        );
    }

    @Test
    void generateComputerHardwareReport_happyPath_buildsReportWithDetectedComponents() throws Exception {
        Set<Cpu> cpus = new HashSet<>();
        Cpu cpu = new Cpu();
        cpu.setName("CPU-1");
        cpu.setManufacturer("ACME");
        cpu.setModel("X1");
        cpu.setCores(8);
        cpu.setThreads(16);
        cpu.setMinClock(2000);
        cpu.setMaxClock(3600);
        cpus.add(cpu);
        Set<Gpu> gpus = new HashSet<>();
        Gpu gpu = new Gpu();
        gpu.setName("GPU-1");
        gpu.setManufacturer("ACME");
        gpu.setModel("G1");
        gpu.setCapacity(8);
        gpu.setMinClock(1000);
        gpu.setMaxClock(1800);
        gpus.add(gpu);
        Set<MemoryModule> memories = new HashSet<>();
        MemoryModule memory = new MemoryModule();
        memory.setName("RAM");
        memory.setManufacturer("ACME");
        memory.setModel("R1");
        memory.setCapacity(16384);
        memory.setBusSpeed(3200);
        memories.add(memory);
        Set<Storage> storages = new HashSet<>();
        Storage storage = new Storage();
        storage.setName("SSD");
        storage.setManufacturer("ACME");
        storage.setModel("S1");
        storage.setCapacity(1000);
        storage.setType(StorageType.SOLID_STATE);
        storages.add(storage);
        Set<NetworkDevice> network = new HashSet<>();
        NetworkDevice nic = new NetworkDevice();
        nic.setName("NIC");
        nic.setManufacturer("ACME");
        nic.setModel("N1");
        network.add(nic);
        Set<GenericComponent> power = new HashSet<>();
        GenericComponent psu = new GenericComponent();
        psu.setType(DeviceType.POWER_SOURCE);
        psu.setName("PSU");
        psu.setManufacturer("ACME");
        psu.setModel("P1");
        power.add(psu);
        Set<GenericComponent> usb = new HashSet<>();
        GenericComponent usbDev = new GenericComponent();
        usbDev.setType(DeviceType.PERIPHERAL);
        usbDev.setName("USB");
        usbDev.setManufacturer("ACME");
        usbDev.setModel("U1");
        usb.add(usbDev);

        when(hardwareUtil.detectCpus()).thenReturn(cpus);
        when(hardwareUtil.detectGpus()).thenReturn(gpus);
        when(hardwareUtil.detectMemoryModules()).thenReturn(memories);
        when(hardwareUtil.detectStorages()).thenReturn(storages);
        when(hardwareUtil.detectNetworkInterfaces()).thenReturn(network);
        when(hardwareUtil.detectPowerSources()).thenReturn(power);
        when(hardwareUtil.detectUsbDevices()).thenReturn(usb);

        HardwareReport report = computerService.generateComputerHardwareReport(loginWallet, true);

        assertNotNull(report);
        assertNotNull(report.getComputer());
        assertEquals(loginWallet, report.getComputer().getName());
        assertEquals(cpus, report.getComputer().getCpus());
        assertEquals(gpus, report.getComputer().getGpus());
        assertEquals(memories, report.getComputer().getMemories());
        assertEquals(storages, report.getComputer().getStorages());
        assertEquals(network, report.getComputer().getNetworkDevices());
        assertTrue(report.getElapsedTime() >= 0);

        verify(hardwareUtil, times(1)).detectCpus();
        verify(hardwareUtil, times(1)).detectGpus();
        verify(hardwareUtil, times(1)).detectMemoryModules();
        verify(hardwareUtil, times(1)).detectStorages();
        verify(hardwareUtil, times(1)).detectNetworkInterfaces();
        verify(hardwareUtil, times(1)).detectPowerSources();
        verify(hardwareUtil, times(1)).detectUsbDevices();
        verifyNoInteractions(dataService, benchmarkUtil, httpUtil, nextGpuWebService);
    }

    @Test
    void generateComputerHardwareReport_whenHardwareDetectionFails_propagatesException() throws Exception {
        when(hardwareUtil.detectCpus()).thenThrow(new RuntimeException("detect failed"));
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> computerService.generateComputerHardwareReport(loginWallet, false));
        assertEquals("detect failed", thrown.getMessage());
    }

    @Test
    void generateComputerBenchmarkReport_happyPath_executesBenchmarksAndSaves() throws Exception {
        Map<String, Object> gpuBench = new HashMap<>();
        gpuBench.put("score", 100);
        Map<String, Object> cpuBench = new HashMap<>();
        cpuBench.put("score", 200);
        Map<String, Object> memoryBench = new HashMap<>();
        memoryBench.put("score", 300);
        Map<String, Object> storageBench = new HashMap<>();
        storageBench.put("score", 400);

        when(benchmarkUtil.benchmarkGpu()).thenReturn(gpuBench);
        when(benchmarkUtil.benchmarkCpu()).thenReturn(cpuBench);
        when(benchmarkUtil.benchmarkMemory(anyBoolean())).thenReturn(memoryBench);
        when(benchmarkUtil.benchmarkStorage(anyBoolean())).thenReturn(storageBench);

        Map<String, Object> networkBench = new HashMap<>();
        networkBench.put("score", 500);
        when(benchmarkUtil.benchmarkNetwork(anyBoolean())).thenReturn(networkBench);

        Provider provider = new Provider();
        provider.setWalletAddress(loginWallet);
        when(dataService.findProviderByWalletAddress(loginWallet)).thenReturn(provider);

        BenchmarkReport reportToSave = new BenchmarkReport();
        when(dataService.saveBenchmarkReport(any(BenchmarkReport.class))).thenReturn(reportToSave);

        BenchmarkReport result = computerService.generateComputerBenchmarkReport(loginWallet, true);

        assertNotNull(result);
        verify(benchmarkUtil).benchmarkGpu();
        verify(benchmarkUtil).benchmarkCpu();
        verify(benchmarkUtil).benchmarkMemory(true);
        verify(benchmarkUtil).benchmarkStorage(true);
        verify(benchmarkUtil).benchmarkNetwork(true);
        verify(dataService).saveBenchmarkReport(any(BenchmarkReport.class));
    }

    @Test
    void generateComputerUsageReport_returnsUsageMap() {
        when(hardwareUtil.readCurrentCpuTemperature()).thenReturn(50.0);
        when(hardwareUtil.readCurrentCpuUsage()).thenReturn(10.0);

        Map<String, Object> result = computerService.generateComputerUsageReport();

        assertEquals(50.0, result.get("cpuTemperature"));
        assertEquals(10.0, result.get("cpuUsage"));
        verify(hardwareUtil).readCurrentCpuTemperature();
        verify(hardwareUtil).readCurrentCpuUsage();
    }

    @Test
    void saveComputer_NewComputer_CallsWebServiceAndSavesLocally() throws Exception {
        Computer detectedHardware = new Computer();
        detectedHardware.setType(ComputerType.PERSONAL_DESKTOP);
        detectedHardware.setCpus(new HashSet<>());
        detectedHardware.setGpus(new HashSet<>());
        detectedHardware.setMemories(new HashSet<>());
        detectedHardware.setStorages(new HashSet<>());
        detectedHardware.setNetworkDevices(new HashSet<>());
        detectedHardware.setOtherComponents(new HashSet<>());

        Provider provider = new Provider();
        provider.setWalletAddress(loginWallet);

        when(computerRepository.findAll()).thenReturn(Collections.emptyList());
        when(dataService.findProviderByWalletAddress(loginWallet)).thenReturn(provider);

        ComputerDto responseDto = new ComputerDto();
        responseDto.setUuid("new-uuid");
        when(nextGpuWebService.createComputer(any(CreateComputerDto.class))).thenReturn(responseDto);

        when(dataService.saveComputer(any(Computer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Computer result = computerService.saveComputer(detectedHardware, loginWallet);

        assertNotNull(result);
        assertEquals("new-uuid", result.getUuid());
        assertEquals(provider, result.getProvider());
        verify(nextGpuWebService).createComputer(any(CreateComputerDto.class));
        verify(dataService).saveComputer(any(Computer.class));
    }

    @Test
    void saveComputer_WithComponents_ResolvesAndDeduplicates() throws Exception {
        Computer detectedHardware = new Computer();
        detectedHardware.setType(ComputerType.SERVER);
        
        Cpu cpu = new Cpu();
        cpu.setManufacturer("Intel");
        cpu.setModel("i7");
        detectedHardware.setCpus(Collections.singleton(cpu));
        detectedHardware.setGpus(new HashSet<>());
        detectedHardware.setMemories(new HashSet<>());
        detectedHardware.setStorages(new HashSet<>());
        detectedHardware.setNetworkDevices(new HashSet<>());
        detectedHardware.setOtherComponents(new HashSet<>());

        Provider provider = new Provider();
        provider.setWalletAddress(loginWallet);

        Computer localComputer = new Computer();
        localComputer.setProvider(provider);

        when(computerRepository.findAll()).thenReturn(Collections.singletonList(localComputer));
        
        ComputerDto responseDto = new ComputerDto();
        responseDto.setUuid("uuid-123");
        when(nextGpuWebService.createComputer(any(CreateComputerDto.class))).thenReturn(responseDto);

        CpuRepository cpuRepository = mock(CpuRepository.class);
        when(dataService.getCpuRepository()).thenReturn(cpuRepository);
        when(cpuRepository.findByManufacturerAndModel("Intel", "i7")).thenReturn(Optional.empty());
        when(dataService.saveCpu(any(Cpu.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock other repositories to avoid NPE if they are called (though they should be empty in this test)
        when(dataService.getGpuRepository()).thenReturn(mock(GpuRepository.class));
        when(dataService.getMemoryModuleRepository()).thenReturn(mock(MemoryModuleRepository.class));
        when(dataService.getStorageRepository()).thenReturn(mock(StorageRepository.class));
        when(dataService.getNetworkDeviceRepository()).thenReturn(mock(NetworkDeviceRepository.class));

        when(dataService.saveComputer(any(Computer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Computer result = computerService.saveComputer(detectedHardware, loginWallet);

        assertNotNull(result);
        assertEquals(1, result.getCpus().size());
        verify(dataService).saveCpu(any(Cpu.class));
        verify(dataService).saveComputer(any(Computer.class));
    }

    @Test
    void saveComputer_whenWebServiceThrows_throwsRuntimeException() throws Exception {
        Computer detectedHardware = new Computer();
        detectedHardware.setCpus(new HashSet<>());
        detectedHardware.setGpus(new HashSet<>());
        detectedHardware.setMemories(new HashSet<>());
        detectedHardware.setStorages(new HashSet<>());
        detectedHardware.setNetworkDevices(new HashSet<>());
        detectedHardware.setOtherComponents(new HashSet<>());

        lenient().when(computerRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(dataService.findProviderByWalletAddress(anyString())).thenReturn(new Provider());
        when(nextGpuWebService.createComputer(any())).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class, () -> computerService.saveComputer(detectedHardware, loginWallet));
    }

    @Test
    void saveComputer_withUnknownAttributes_skipsThem() throws Exception {
        Computer detectedHardware = getTestHardware();

        lenient().when(computerRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(dataService.findProviderByWalletAddress(loginWallet)).thenReturn(new Provider());
        when(nextGpuWebService.createComputer(any())).thenReturn(new ComputerDto());
        lenient().when(computerAttributeTypeRepository.findByName("unknown_attr")).thenReturn(Optional.empty());
        lenient().when(dataService.getComputerAttributeTypeRepository()).thenReturn(computerAttributeTypeRepository);
        lenient().when(dataService.saveComputer(any())).thenAnswer(i -> i.getArgument(0));

        Computer result = computerService.saveComputer(detectedHardware, loginWallet);

        assertTrue(result.getComputerAttributes().isEmpty());
    }

    private static @NotNull Computer getTestHardware() {
        Computer detectedHardware = new Computer();
        detectedHardware.setCpus(new HashSet<>());
        detectedHardware.setGpus(new HashSet<>());
        detectedHardware.setMemories(new HashSet<>());
        detectedHardware.setStorages(new HashSet<>());
        detectedHardware.setNetworkDevices(new HashSet<>());
        detectedHardware.setOtherComponents(new HashSet<>());

        ComputerAttributeType type = new ComputerAttributeType();
        type.setName("unknown_attr");
        detectedHardware.setComputerAttributes(Map.of(type, "value"));
        return detectedHardware;
    }

    @Test
    void saveComputer_withNullManufacturerOrModel_stillDeduplicates() throws Exception {
        Computer detectedHardware = new Computer();
        Cpu cpu1 = new Cpu();
        cpu1.setManufacturer(null);
        cpu1.setModel(null);
        Cpu cpu2 = new Cpu();
        cpu2.setManufacturer(null);
        cpu2.setModel(null);
        detectedHardware.setCpus(Set.of(cpu1, cpu2));
        detectedHardware.setGpus(new HashSet<>());
        detectedHardware.setMemories(new HashSet<>());
        detectedHardware.setStorages(new HashSet<>());
        detectedHardware.setNetworkDevices(new HashSet<>());
        detectedHardware.setOtherComponents(new HashSet<>());

        lenient().when(computerRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(dataService.findProviderByWalletAddress(loginWallet)).thenReturn(new Provider());
        when(nextGpuWebService.createComputer(any())).thenReturn(new ComputerDto());
        
        CpuRepository cpuRepository = mock(CpuRepository.class);
        lenient().when(dataService.getCpuRepository()).thenReturn(cpuRepository);
        lenient().when(cpuRepository.findByManufacturerAndModel(null, null)).thenReturn(Optional.empty());
        lenient().when(dataService.saveCpu(any())).thenAnswer(i -> i.getArgument(0));
        
        lenient().when(dataService.getGpuRepository()).thenReturn(mock(GpuRepository.class));
        lenient().when(dataService.getMemoryModuleRepository()).thenReturn(mock(MemoryModuleRepository.class));
        lenient().when(dataService.getStorageRepository()).thenReturn(mock(StorageRepository.class));
        lenient().when(dataService.getNetworkDeviceRepository()).thenReturn(mock(NetworkDeviceRepository.class));
        lenient().when(dataService.saveComputer(any())).thenAnswer(i -> i.getArgument(0));

        Computer result = computerService.saveComputer(detectedHardware, loginWallet);

        assertEquals(1, result.getCpus().size());
    }

    @Test
    void saveComputerAttributeTypes_CallsRepository() {
        ComputerAttributeTypeDto dto = new ComputerAttributeTypeDto();
        dto.setName("LAST_AUDIT_STATUS");
        dto.setDescription("Last audit status");
        dto.setDatatype("java.lang.String");

        List<ComputerAttributeTypeDto> types = Collections.singletonList(dto);

        when(dataService.getComputerAttributeTypeRepository()).thenReturn(computerAttributeTypeRepository);

        computerService.saveComputerAttributeTypes(types);

        verify(computerAttributeTypeRepository).save(argThat(entity ->
                entity != null
                        && "LAST_AUDIT_STATUS".equals(entity.getName())
                        && "Last audit status".equals(entity.getDescription())
                        && "java.lang.String".equals(entity.getDatatype())
        ));
    }
}
