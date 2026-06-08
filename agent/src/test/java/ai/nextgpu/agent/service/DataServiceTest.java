package ai.nextgpu.agent.service;

import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.agent.repository.*;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.report.HardwareReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataServiceTest {

    @Mock
    private BenchmarkReportRepository benchmarkReportRepository;

    @Mock
    private ComputerRepository computerRepository;

    @Mock
    private CpuRepository cpuRepository;

    @Mock
    private GenericComponentRepository genericComponentRepository;

    @Mock
    private GlobalPropertyRepository globalPropertyRepository;

    @Mock
    private GpuRepository gpuRepository;

    @Mock
    private HardwareReportRepository hardwareReportRepository;

    @Mock
    private MemoryModuleRepository memoryModuleRepository;

    @Mock
    private NetworkDeviceRepository networkDeviceRepository;

    @Mock
    private StorageRepository storageRepository;

    @InjectMocks
    private DataService dataService;

    private Computer testComputer;
    private Cpu testCpu;
    private Gpu testGpu;
    private MemoryModule testMemoryModule;
    private Storage testStorage;
    private NetworkDevice testNetworkDevice;
    private GenericComponent testGenericComponent;
    private GlobalProperty testGlobalProperty;
    private HardwareReport testHardwareReport;
    private BenchmarkReport testBenchmarkReport;

    @BeforeEach
    void setUp() {
        // Initialize test entities
        testComputer = new Computer();
        testComputer.setName("Test Computer");

        testCpu = new Cpu();
        testCpu.setName("Test CPU");
        testCpu.setManufacturer("Intel");
        testCpu.setModel("i9-9900K");

        testGpu = new Gpu();
        testGpu.setName("Test GPU");
        testGpu.setManufacturer("NVIDIA");
        testGpu.setModel("RTX 3080");
        testGpu.setArchitecture(GpuArchitecture.AMPERE);

        testMemoryModule = new MemoryModule();
        testMemoryModule.setName("Test Memory");
        testMemoryModule.setManufacturer("Corsair");
        testMemoryModule.setModel("Vengeance RGB Pro");

        testStorage = new Storage();
        testStorage.setName("Test Storage");
        testStorage.setManufacturer("Samsung");
        testStorage.setModel("970 EVO Plus");

        testNetworkDevice = new NetworkDevice();
        testNetworkDevice.setName("Test Network Device");
        testNetworkDevice.setManufacturer("Intel");
        testNetworkDevice.setModel("AX200");

        testGenericComponent = new GenericComponent();
        testGenericComponent.setName("Test Generic Component");
        testGenericComponent.setManufacturer("Generic");
        testGenericComponent.setModel("Model X");

        testGlobalProperty = new GlobalProperty();
        testGlobalProperty.setName("test.property");
        testGlobalProperty.setValueReference("test value");

        testHardwareReport = new HardwareReport();
        testHardwareReport.setComputer(testComputer);

        testBenchmarkReport = new BenchmarkReport();
        testBenchmarkReport.setComputer(testComputer);
        testBenchmarkReport.setElapsedTime(1000L);
    }

    @Test
    void testSaveComputer() {
        when(computerRepository.save(any(Computer.class))).thenReturn(testComputer);

        Computer savedComputer = dataService.saveComputer(testComputer);

        assertNotNull(savedComputer);
        assertEquals(testComputer.getName(), savedComputer.getName());
        verify(computerRepository, times(1)).save(testComputer);
    }

    @Test
    void testFindComputerById() {
        when(computerRepository.findById(1L)).thenReturn(Optional.of(testComputer));

        Optional<Computer> foundComputer = dataService.findComputerById(1L);

        assertTrue(foundComputer.isPresent());
        assertEquals(testComputer.getName(), foundComputer.get().getName());
        verify(computerRepository, times(1)).findById(1L);
    }

    @Test
    void testFindComputerByName() {
        when(computerRepository.findByName("Test Computer")).thenReturn(Optional.of(testComputer));

        Optional<Computer> foundComputer = dataService.findComputerByName("Test Computer");

        assertTrue(foundComputer.isPresent());
        assertEquals(testComputer.getName(), foundComputer.get().getName());
        verify(computerRepository, times(1)).findByName("Test Computer");
    }

    @Test
    void testFindAllComputers() {
        List<Computer> computers = Arrays.asList(testComputer);
        when(computerRepository.findAll()).thenReturn(computers);

        List<Computer> foundComputers = dataService.findAllComputers();

        assertNotNull(foundComputers);
        assertEquals(1, foundComputers.size());
        assertEquals(testComputer.getName(), foundComputers.get(0).getName());
        verify(computerRepository, times(1)).findAll();
    }

    @Test
    void testDeleteComputer() {
        doNothing().when(computerRepository).delete(testComputer);

        dataService.deleteComputer(testComputer);

        verify(computerRepository, times(1)).delete(testComputer);
    }

    @Test
    void testDeleteComputerById() {
        doNothing().when(computerRepository).deleteById(1L);

        dataService.deleteComputerById(1L);

        verify(computerRepository, times(1)).deleteById(1L);
    }

    @Test
    void testSaveCpu() {
        when(cpuRepository.save(any(Cpu.class))).thenReturn(testCpu);

        Cpu savedCpu = dataService.saveCpu(testCpu);

        assertNotNull(savedCpu);
        assertEquals(testCpu.getName(), savedCpu.getName());
        assertEquals(testCpu.getManufacturer(), savedCpu.getManufacturer());
        assertEquals(testCpu.getModel(), savedCpu.getModel());
        verify(cpuRepository, times(1)).save(testCpu);
    }

    @Test
    void testFindCpuById() {
        when(cpuRepository.findById(1L)).thenReturn(Optional.of(testCpu));

        Optional<Cpu> foundCpu = dataService.findCpuById(1L);

        assertTrue(foundCpu.isPresent());
        assertEquals(testCpu.getName(), foundCpu.get().getName());
        assertEquals(testCpu.getManufacturer(), foundCpu.get().getManufacturer());
        assertEquals(testCpu.getModel(), foundCpu.get().getModel());
        verify(cpuRepository, times(1)).findById(1L);
    }

    @Test
    void testFindCpuByName() {
        when(cpuRepository.findByName("Test CPU")).thenReturn(Optional.of(testCpu));

        Optional<Cpu> foundCpu = dataService.findCpuByName("Test CPU");

        assertTrue(foundCpu.isPresent());
        assertEquals(testCpu.getName(), foundCpu.get().getName());
        assertEquals(testCpu.getManufacturer(), foundCpu.get().getManufacturer());
        assertEquals(testCpu.getModel(), foundCpu.get().getModel());
        verify(cpuRepository, times(1)).findByName("Test CPU");
    }

    @Test
    void testFindCpusByManufacturer() {
        List<Cpu> cpus = Arrays.asList(testCpu);
        when(cpuRepository.findByManufacturer("Intel")).thenReturn(cpus);

        List<Cpu> foundCpus = dataService.findCpusByManufacturer("Intel");

        assertNotNull(foundCpus);
        assertEquals(1, foundCpus.size());
        assertEquals(testCpu.getName(), foundCpus.get(0).getName());
        assertEquals(testCpu.getManufacturer(), foundCpus.get(0).getManufacturer());
        assertEquals(testCpu.getModel(), foundCpus.get(0).getModel());
        verify(cpuRepository, times(1)).findByManufacturer("Intel");
    }

    @Test
    void testFindCpusByModel() {
        List<Cpu> cpus = Arrays.asList(testCpu);
        when(cpuRepository.findByModel("i9-9900K")).thenReturn(cpus);

        List<Cpu> foundCpus = dataService.findCpusByModel("i9-9900K");

        assertNotNull(foundCpus);
        assertEquals(1, foundCpus.size());
        assertEquals(testCpu.getName(), foundCpus.get(0).getName());
        assertEquals(testCpu.getManufacturer(), foundCpus.get(0).getManufacturer());
        assertEquals(testCpu.getModel(), foundCpus.get(0).getModel());
        verify(cpuRepository, times(1)).findByModel("i9-9900K");
    }

    @Test
    void testSaveGpu() {
        when(gpuRepository.save(any(Gpu.class))).thenReturn(testGpu);

        Gpu savedGpu = dataService.saveGpu(testGpu);

        assertNotNull(savedGpu);
        assertEquals(testGpu.getName(), savedGpu.getName());
        assertEquals(testGpu.getManufacturer(), savedGpu.getManufacturer());
        assertEquals(testGpu.getModel(), savedGpu.getModel());
        assertEquals(testGpu.getArchitecture(), savedGpu.getArchitecture());
        verify(gpuRepository, times(1)).save(testGpu);
    }

    @Test
    void testFindGpusByArchitecture() {
        List<Gpu> gpus = Arrays.asList(testGpu);
        when(gpuRepository.findByArchitecture(GpuArchitecture.AMPERE)).thenReturn(gpus);

        List<Gpu> foundGpus = dataService.findGpusByArchitecture(GpuArchitecture.AMPERE);

        assertNotNull(foundGpus);
        assertEquals(1, foundGpus.size());
        assertEquals(testGpu.getName(), foundGpus.get(0).getName());
        assertEquals(testGpu.getManufacturer(), foundGpus.get(0).getManufacturer());
        assertEquals(testGpu.getModel(), foundGpus.get(0).getModel());
        assertEquals(testGpu.getArchitecture(), foundGpus.get(0).getArchitecture());
        verify(gpuRepository, times(1)).findByArchitecture(GpuArchitecture.AMPERE);
    }

    @Test
    void testSaveGlobalProperty() {
        when(globalPropertyRepository.save(any(GlobalProperty.class))).thenReturn(testGlobalProperty);

        GlobalProperty savedProperty = dataService.saveGlobalProperty(testGlobalProperty);

        assertNotNull(savedProperty);
        assertEquals(testGlobalProperty.getName(), savedProperty.getName());
        assertEquals(testGlobalProperty.getValueReference(), savedProperty.getValueReference());
        verify(globalPropertyRepository, times(1)).save(testGlobalProperty);
    }

    @Test
    void testFindGlobalPropertyByName() {
        when(globalPropertyRepository.findByName("test.property")).thenReturn(Optional.of(testGlobalProperty));

        Optional<GlobalProperty> foundProperty = dataService.findGlobalPropertyByName("test.property");

        assertTrue(foundProperty.isPresent());
        assertEquals(testGlobalProperty.getName(), foundProperty.get().getName());
        assertEquals(testGlobalProperty.getValueReference(), foundProperty.get().getValueReference());
        verify(globalPropertyRepository, times(1)).findByName("test.property");
    }

    @Test
    void testSaveHardwareReport() {
        when(hardwareReportRepository.save(any(HardwareReport.class))).thenReturn(testHardwareReport);

        HardwareReport savedReport = dataService.saveHardwareReport(testHardwareReport);

        assertNotNull(savedReport);
        assertEquals(testHardwareReport.getComputer(), savedReport.getComputer());
        verify(hardwareReportRepository, times(1)).save(testHardwareReport);
    }

    @Test
    void testSaveBenchmarkReport() {
        when(benchmarkReportRepository.save(any(BenchmarkReport.class))).thenReturn(testBenchmarkReport);

        BenchmarkReport savedReport = dataService.saveBenchmarkReport(testBenchmarkReport);

        assertNotNull(savedReport);
        verify(benchmarkReportRepository, times(1)).save(testBenchmarkReport);
    }
}
