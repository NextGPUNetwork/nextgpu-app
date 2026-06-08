package ai.nextgpu.agent.service;

import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.report.HardwareReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ComputerServiceTest {

    @Mock private DataService dataService;
    @Mock private HardwareUtil hardwareUtil;
    @Mock private BenchmarkUtil benchmarkUtil;
    @Mock private HttpUtil httpUtil;
    @Mock private NextGpuWebService nextGpuWebService;

    private ComputerService computerService;

    private final String loginWallet = "wallet-123";

    @BeforeEach
    void setUp() {
        computerService = new ComputerService(
                dataService,
                hardwareUtil,
                benchmarkUtil,
                httpUtil,
                nextGpuWebService
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

        Provider provider = new Provider();
        provider.setWalletAddress(loginWallet);
        when(dataService.findProviderByWalletAddress(loginWallet)).thenReturn(provider);

        Computer computer = new Computer();
        computer.setProvider(provider);
        when(dataService.findAllComputers()).thenReturn(Collections.singletonList(computer));

        BenchmarkReport reportToSave = new BenchmarkReport();
        when(dataService.saveBenchmarkReport(any(BenchmarkReport.class))).thenReturn(reportToSave);

        BenchmarkReport result = computerService.generateComputerBenchmarkReport(loginWallet, true);

        assertNotNull(result);
        verify(benchmarkUtil).benchmarkGpu();
        verify(benchmarkUtil).benchmarkCpu();
        verify(benchmarkUtil).benchmarkMemory(true);
        verify(benchmarkUtil).benchmarkStorage(true);
        verify(dataService).saveBenchmarkReport(any(BenchmarkReport.class));
        verifyNoInteractions(httpUtil);
    }
}
