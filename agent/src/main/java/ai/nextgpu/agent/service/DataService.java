package ai.nextgpu.agent.service;

import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.agent.repository.*;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.report.HardwareReport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing data operations across all entities.
 * Provides CRUD operations and search methods for each entity.
 */
@Slf4j
@Getter
@Service
public class DataService {

    private final BenchmarkReportRepository benchmarkReportRepository;
    private final ComputerRepository computerRepository;
    private final CpuRepository cpuRepository;
    private final GenericComponentRepository genericComponentRepository;
    private final GlobalPropertyRepository globalPropertyRepository;
    private final GpuRepository gpuRepository;
    private final HardwareReportRepository hardwareReportRepository;
    private final MemoryModuleRepository memoryModuleRepository;
    private final NetworkDeviceRepository networkDeviceRepository;
    private final StorageRepository storageRepository;
    private final ProviderRepository providerRepository;
    private final ComputerAttributeTypeRepository computerAttributeTypeRepository;

    @Autowired
    public DataService(
            BenchmarkReportRepository benchmarkReportRepository,
            ComputerRepository computerRepository,
            CpuRepository cpuRepository,
            GenericComponentRepository genericComponentRepository,
            GlobalPropertyRepository globalPropertyRepository,
            GpuRepository gpuRepository,
            HardwareReportRepository hardwareReportRepository,
            MemoryModuleRepository memoryModuleRepository,
            NetworkDeviceRepository networkDeviceRepository,
            StorageRepository storageRepository,
            ProviderRepository providerRepository, ComputerAttributeTypeRepository computerAttributeTypeRepository) {
        this.benchmarkReportRepository = benchmarkReportRepository;
        this.computerRepository = computerRepository;
        this.cpuRepository = cpuRepository;
        this.genericComponentRepository = genericComponentRepository;
        this.globalPropertyRepository = globalPropertyRepository;
        this.gpuRepository = gpuRepository;
        this.hardwareReportRepository = hardwareReportRepository;
        this.memoryModuleRepository = memoryModuleRepository;
        this.networkDeviceRepository = networkDeviceRepository;
        this.storageRepository = storageRepository;
        this.providerRepository = providerRepository;
        this.computerAttributeTypeRepository = computerAttributeTypeRepository;
    }

    /* ************************ */
    /* * Computer Operations * */
    /* ************************ */

    /**
     * Save a computer entity.
     *
     * @param computer the computer to save
     * @return the saved computer
     */
    @Transactional
    public Computer saveComputer(Computer computer) {
        if (computer.getId() != null) {
            computer.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving computer: {}", computer);
        return computerRepository.save(computer);
    }

    /**
     * Find a computer by its ID.
     *
     * @param id the ID of the computer to find
     * @return an Optional containing the computer if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Computer> findComputerById(Long id) {
        return computerRepository.findById(id);
    }

    /**
     * Find a computer by its name.
     *
     * @param name the name of the computer to find
     * @return an Optional containing the computer if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Computer> findComputerByName(String name) {
        return computerRepository.findByName(name);
    }

    /**
     * Find all computers.
     *
     * @return a list of all computers
     */
    @Transactional(readOnly = true)
    public List<Computer> findAllComputers() {
        return computerRepository.findAll();
    }

    /**
     * Delete a computer.
     *
     * @param computer the computer to delete
     */
    @Transactional
    public void deleteComputer(Computer computer) {
        computerRepository.delete(computer);
    }

    /**
     * Delete a computer by its ID.
     *
     * @param id the ID of the computer to delete
     */
    @Transactional
    public void deleteComputerById(Long id) {
        computerRepository.deleteById(id);
    }

    /* ************************ */
    /* * CPU Operations * */
    /* ************************ */

    /**
     * Save a CPU entity.
     *
     * @param cpu the CPU to save
     * @return the saved CPU
     */
    @Transactional
    public Cpu saveCpu(Cpu cpu) {
        if (cpu.getId() != null) {
            cpu.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving CPU: {}", cpu);
        return cpuRepository.save(cpu);
    }

    /**
     * Find a CPU by its ID.
     *
     * @param id the ID of the CPU to find
     * @return an Optional containing the CPU if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Cpu> findCpuById(Long id) {
        return cpuRepository.findById(id);
    }

    /**
     * Find a CPU by its name.
     *
     * @param name the name of the CPU to find
     * @return an Optional containing the CPU if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Cpu> findCpuByName(String name) {
        return cpuRepository.findByName(name);
    }

    /**
     * Find CPUs by manufacturer.
     *
     * @param manufacturer the manufacturer name to search for
     * @return a list of CPUs from the specified manufacturer
     */
    @Transactional(readOnly = true)
    public List<Cpu> findCpusByManufacturer(String manufacturer) {
        return cpuRepository.findByManufacturer(manufacturer);
    }

    /**
     * Find CPUs by model.
     *
     * @param model the model name to search for
     * @return a list of CPUs with the specified model
     */
    @Transactional(readOnly = true)
    public List<Cpu> findCpusByModel(String model) {
        return cpuRepository.findByModel(model);
    }

    /**
     * Find all CPUs.
     *
     * @return a list of all CPUs
     */
    @Transactional(readOnly = true)
    public List<Cpu> findAllCpus() {
        return cpuRepository.findAll();
    }
    /**
     * Delete a CPU.
     *
     * @param cpu the CPU to delete
     */
    @Transactional
    public void deleteCpu(Cpu cpu) {
        cpuRepository.delete(cpu);
    }

    /**
     * Delete a CPU by its ID.
     *
     * @param id the ID of the CPU to delete
     */
    @Transactional
    public void deleteCpuById(Long id) {
        cpuRepository.deleteById(id);
    }

    /* ************************ */
    /* * GPU Operations * */
    /* ************************ */

    /**
     * Save a GPU entity.
     *
     * @param gpu the GPU to save
     * @return the saved GPU
     */
    @Transactional
    public Gpu saveGpu(Gpu gpu) {
        if (gpu.getId() != null) {
            gpu.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving GPU: {}", gpu);
        return gpuRepository.save(gpu);
    }

    /**
     * Find a GPU by its ID.
     *
     * @param id the ID of the GPU to find
     * @return an Optional containing the GPU if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Gpu> findGpuById(Long id) {
        return gpuRepository.findById(id);
    }

    /**
     * Find a GPU by its name.
     *
     * @param name the name of the GPU to find
     * @return an Optional containing the GPU if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Gpu> findGpuByName(String name) {
        return gpuRepository.findByName(name);
    }

    /**
     * Find GPUs by manufacturer.
     *
     * @param manufacturer the manufacturer name to search for
     * @return a list of GPUs from the specified manufacturer
     */
    @Transactional(readOnly = true)
    public List<Gpu> findGpusByManufacturer(String manufacturer) {
        return gpuRepository.findByManufacturer(manufacturer);
    }

    /**
     * Find GPUs by model.
     *
     * @param model the model name to search for
     * @return a list of GPUs with the specified model
     */
    @Transactional(readOnly = true)
    public List<Gpu> findGpusByModel(String model) {
        return gpuRepository.findByModel(model);
    }

    /**
     * Find GPUs by architecture.
     *
     * @param architecture the GPU architecture to search for
     * @return a list of GPUs with the specified architecture
     */
    @Transactional(readOnly = true)
    public List<Gpu> findGpusByArchitecture(GpuArchitecture architecture) {
        return gpuRepository.findByArchitecture(architecture);
    }

    /**
     * Find all GPUs.
     *
     * @return a list of all GPUs
     */
    @Transactional(readOnly = true)
    public List<Gpu> findAllGpus() {
        return gpuRepository.findAll();
    }

    /**
     * Delete a GPU.
     *
     * @param gpu the GPU to delete
     */
    @Transactional
    public void deleteGpu(Gpu gpu) {
        gpuRepository.delete(gpu);
    }

    /**
     * Delete a GPU by its ID.
     *
     * @param id the ID of the GPU to delete
     */
    @Transactional
    public void deleteGpuById(Long id) {
        gpuRepository.deleteById(id);
    }

    /* ************************ */
    /* * Memory Module Operations * */
    /* ************************ */

    /**
     * Save a memory module entity.
     *
     * @param memoryModule the memory module to save
     * @return the saved memory module
     */
    @Transactional
    public MemoryModule saveMemoryModule(MemoryModule memoryModule) {
        if (memoryModule.getId() != null) {
            memoryModule.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving memory module: {}", memoryModule);
        return memoryModuleRepository.save(memoryModule);
    }

    /**
     * Find a memory module by its ID.
     *
     * @param id the ID of the memory module to find
     * @return an Optional containing the memory module if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<MemoryModule> findMemoryModuleById(Long id) {
        return memoryModuleRepository.findById(id);
    }

    /**
     * Find a memory module by its name.
     *
     * @param name the name of the memory module to find
     * @return an Optional containing the memory module if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<MemoryModule> findMemoryModuleByName(String name) {
        return memoryModuleRepository.findByName(name);
    }

    /**
     * Find memory modules by manufacturer.
     *
     * @param manufacturer the manufacturer name to search for
     * @return a list of memory modules from the specified manufacturer
     */
    @Transactional(readOnly = true)
    public List<MemoryModule> findMemoryModulesByManufacturer(String manufacturer) {
        return memoryModuleRepository.findByManufacturer(manufacturer);
    }

    /**
     * Find memory modules by model.
     *
     * @param model the model name to search for
     * @return a list of memory modules with the specified model
     */
    @Transactional(readOnly = true)
    public List<MemoryModule> findMemoryModulesByModel(String model) {
        return memoryModuleRepository.findByModel(model);
    }

    /**
     * Find all memory modules.
     *
     * @return a list of all memory modules
     */
    @Transactional(readOnly = true)
    public List<MemoryModule> findAllMemoryModules() {
        return memoryModuleRepository.findAll();
    }

    /**
     * Delete a memory module.
     *
     * @param memoryModule the memory module to delete
     */
    @Transactional
    public void deleteMemoryModule(MemoryModule memoryModule) {
        memoryModuleRepository.delete(memoryModule);
    }

    /**
     * Delete a memory module by its ID.
     *
     * @param id the ID of the memory module to delete
     */
    @Transactional
    public void deleteMemoryModuleById(Long id) {
        memoryModuleRepository.deleteById(id);
    }

    /* ************************ */
    /* * Storage Operations * */
    /* ************************ */

    /**
     * Save a storage entity.
     *
     * @param storage the storage to save
     * @return the saved storage
     */
    @Transactional
    public Storage saveStorage(Storage storage) {
        if (storage.getId() != null) {
            storage.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving storage: {}", storage);
        return storageRepository.save(storage);
    }

    /**
     * Find a storage by its ID.
     *
     * @param id the ID of the storage to find
     * @return an Optional containing the storage if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Storage> findStorageById(Long id) {
        return storageRepository.findById(id);
    }

    /**
     * Find a storage by its name.
     *
     * @param name the name of the storage to find
     * @return an Optional containing the storage if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<Storage> findStorageByName(String name) {
        return storageRepository.findByName(name);
    }

    /**
     * Find storages by manufacturer.
     *
     * @param manufacturer the manufacturer name to search for
     * @return a list of storages from the specified manufacturer
     */
    @Transactional(readOnly = true)
    public List<Storage> findStoragesByManufacturer(String manufacturer) {
        return storageRepository.findByManufacturer(manufacturer);
    }

    /**
     * Find storages by model.
     *
     * @param model the model name to search for
     * @return a list of storages with the specified model
     */
    @Transactional(readOnly = true)
    public List<Storage> findStoragesByModel(String model) {
        return storageRepository.findByModel(model);
    }

    /**
     * Find all storages.
     *
     * @return a list of all storages
     */
    @Transactional(readOnly = true)
    public List<Storage> findAllStorages() {
        return storageRepository.findAll();
    }

    /**
     * Delete a storage.
     *
     * @param storage the storage to delete
     */
    @Transactional
    public void deleteStorage(Storage storage) {
        storageRepository.delete(storage);
    }

    /**
     * Delete a storage by its ID.
     *
     * @param id the ID of the storage to delete
     */
    @Transactional
    public void deleteStorageById(Long id) {
        storageRepository.deleteById(id);
    }

    /* ************************ */
    /* * Network Device Operations * */
    /* ************************ */

    /**
     * Save a network device entity.
     *
     * @param networkDevice the network device to save
     * @return the saved network device
     */
    @Transactional
    public NetworkDevice saveNetworkDevice(NetworkDevice networkDevice) {
        if (networkDevice.getId() != null) {
            networkDevice.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving network device: {}", networkDevice);
        return networkDeviceRepository.save(networkDevice);
    }

    /**
     * Find a network device by its ID.
     *
     * @param id the ID of the network device to find
     * @return an Optional containing the network device if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<NetworkDevice> findNetworkDeviceById(Long id) {
        return networkDeviceRepository.findById(id);
    }

    /**
     * Find a network device by its name.
     *
     * @param name the name of the network device to find
     * @return an Optional containing the network device if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<NetworkDevice> findNetworkDeviceByName(String name) {
        return networkDeviceRepository.findByName(name);
    }

    /**
     * Find network devices by manufacturer.
     *
     * @param manufacturer the manufacturer name to search for
     * @return a list of network devices from the specified manufacturer
     */
    @Transactional(readOnly = true)
    public List<NetworkDevice> findNetworkDevicesByManufacturer(String manufacturer) {
        return networkDeviceRepository.findByManufacturer(manufacturer);
    }

    /**
     * Find network devices by model.
     *
     * @param model the model name to search for
     * @return a list of network devices with the specified model
     */
    @Transactional(readOnly = true)
    public List<NetworkDevice> findNetworkDevicesByModel(String model) {
        return networkDeviceRepository.findByModel(model);
    }

    /**
     * Find all network devices.
     *
     * @return a list of all network devices
     */
    @Transactional(readOnly = true)
    public List<NetworkDevice> findAllNetworkDevices() {
        return networkDeviceRepository.findAll();
    }

    /**
     * Delete a network device.
     *
     * @param networkDevice the network device to delete
     */
    @Transactional
    public void deleteNetworkDevice(NetworkDevice networkDevice) {
        networkDeviceRepository.delete(networkDevice);
    }

    /**
     * Delete a network device by its ID.
     *
     * @param id the ID of the network device to delete
     */
    @Transactional
    public void deleteNetworkDeviceById(Long id) {
        networkDeviceRepository.deleteById(id);
    }

    /* ************************ */
    /* * Generic Component Operations * */
    /* ************************ */

    /**
     * Save a generic component entity.
     *
     * @param genericComponent the generic component to save
     * @return the saved generic component
     */
    @Transactional
    public GenericComponent saveGenericComponent(GenericComponent genericComponent) {
        if (genericComponent.getId() != null) {
            genericComponent.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving generic component: {}", genericComponent);
        return genericComponentRepository.save(genericComponent);
    }

    /**
     * Find a generic component by its ID.
     *
     * @param id the ID of the generic component to find
     * @return an Optional containing the generic component if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<GenericComponent> findGenericComponentById(Long id) {
        return genericComponentRepository.findById(id);
    }

    /**
     * Find a generic component by its name.
     *
     * @param name the name of the generic component to find
     * @return an Optional containing the generic component if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<GenericComponent> findGenericComponentByName(String name) {
        return genericComponentRepository.findByName(name);
    }

    /**
     * Find generic components by manufacturer.
     *
     * @param manufacturer the manufacturer name to search for
     * @return a list of generic components from the specified manufacturer
     */
    @Transactional(readOnly = true)
    public List<GenericComponent> findGenericComponentsByManufacturer(String manufacturer) {
        return genericComponentRepository.findByManufacturer(manufacturer);
    }

    /**
     * Find generic components by model.
     *
     * @param model the model name to search for
     * @return a list of generic components with the specified model
     */
    @Transactional(readOnly = true)
    public List<GenericComponent> findGenericComponentsByModel(String model) {
        return genericComponentRepository.findByModel(model);
    }

    /**
     * Find all generic components.
     *
     * @return a list of all generic components
     */
    @Transactional(readOnly = true)
    public List<GenericComponent> findAllGenericComponents() {
        return genericComponentRepository.findAll();
    }

    /**
     * Delete a generic component.
     *
     * @param genericComponent the generic component to delete
     */
    @Transactional
    public void deleteGenericComponent(GenericComponent genericComponent) {
        genericComponentRepository.delete(genericComponent);
    }

    /**
     * Delete a generic component by its ID.
     *
     * @param id the ID of the generic component to delete
     */
    @Transactional
    public void deleteGenericComponentById(Long id) {
        genericComponentRepository.deleteById(id);
    }

    /* ************************ */
    /* * Global Property Operations * */
    /* ************************ */

    /**
     * Save a global property entity.
     *
     * @param globalProperty the global property to save
     * @return the saved global property
     */
    @Transactional
    public GlobalProperty saveGlobalProperty(GlobalProperty globalProperty) {
        if (globalProperty.getId() != null) {
            globalProperty.setDateUpdated(LocalDateTime.now());
        }
        log.debug("Saving global property: {}", globalProperty);
        return globalPropertyRepository.save(globalProperty);
    }

    /**
     * Find a global property by its ID.
     *
     * @param id the ID of the global property to find
     * @return an Optional containing the global property if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<GlobalProperty> findGlobalPropertyById(Long id) {
        return globalPropertyRepository.findById(id);
    }

    /**
     * Find a global property by its name.
     *
     * @param name the name of the global property to find
     * @return an Optional containing the global property if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<GlobalProperty> findGlobalPropertyByName(String name) {
        return globalPropertyRepository.findByName(name);
    }

    /**
     * Find all global properties.
     *
     * @return a list of all global properties
     */
    @Transactional(readOnly = true)
    public List<GlobalProperty> findAllGlobalProperties() {
        return globalPropertyRepository.findAll();
    }

    /**
     * Delete a global property.
     *
     * @param globalProperty the global property to delete
     */
    @Transactional
    public void deleteGlobalProperty(GlobalProperty globalProperty) {
        globalPropertyRepository.delete(globalProperty);
    }

    /**
     * Delete a global property by its ID.
     *
     * @param id the ID of the global property to delete
     */
    @Transactional
    public void deleteGlobalPropertyById(Long id) {
        globalPropertyRepository.deleteById(id);
    }

    /* ************************ */
    /* * Hardware Report Operations * */
    /* ************************ */

    /**
     * Save a hardware report entity.
     *
     * @param hardwareReport the hardware report to save
     * @return the saved hardware report
     */
    @Transactional
    public HardwareReport saveHardwareReport(HardwareReport hardwareReport) {
        log.debug("Saving hardware report: {}", hardwareReport);
        return hardwareReportRepository.save(hardwareReport);
    }

    /**
     * Find a hardware report by its ID.
     *
     * @param id the ID of the hardware report to find
     * @return an Optional containing the hardware report if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<HardwareReport> findHardwareReportById(Long id) {
        return hardwareReportRepository.findById(id);
    }

    /**
     * Find all hardware reports.
     *
     * @return a list of all hardware reports
     */
    @Transactional(readOnly = true)
    public List<HardwareReport> findAllHardwareReports() {
        return hardwareReportRepository.findAll();
    }

    /**
     * Delete a hardware report.
     *
     * @param hardwareReport the hardware report to delete
     */
    @Transactional
    public void deleteHardwareReport(HardwareReport hardwareReport) {
        hardwareReportRepository.delete(hardwareReport);
    }

    /**
     * Delete a hardware report by its ID.
     *
     * @param id the ID of the hardware report to delete
     */
    @Transactional
    public void deleteHardwareReportById(Long id) {
        hardwareReportRepository.deleteById(id);
    }

    /* ************************ */
    /* * Benchmark Report Operations * */
    /* ************************ */

    /**
     * Save a benchmark report entity.
     *
     * @param benchmarkReport the benchmark report to save
     * @return the saved benchmark report
     */
    @Transactional
    public BenchmarkReport saveBenchmarkReport(BenchmarkReport benchmarkReport) {
        log.debug("Saving benchmark report: {}", benchmarkReport);
        return benchmarkReportRepository.save(benchmarkReport);
    }

    /**
     * Find a benchmark report by its ID.
     *
     * @param id the ID of the benchmark report to find
     * @return an Optional containing the benchmark report if found, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<BenchmarkReport> findBenchmarkReportById(Long id) {
        return benchmarkReportRepository.findById(id);
    }

    /**
     * Find all benchmark reports.
     *
     * @return a list of all benchmark reports
     */
    @Transactional(readOnly = true)
    public List<BenchmarkReport> findAllBenchmarkReports() {
        return benchmarkReportRepository.findAll();
    }

    /**
     * Delete a benchmark report.
     *
     * @param benchmarkReport the benchmark report to delete
     */
    @Transactional
    public void deleteBenchmarkReport(BenchmarkReport benchmarkReport) {
        benchmarkReportRepository.delete(benchmarkReport);
    }

    /**
     * Delete a benchmark report by its ID.
     *
     * @param id the ID of the benchmark report to delete
     */
    @Transactional
    public void deleteBenchmarkReportById(Long id) {
        benchmarkReportRepository.deleteById(id);
    }

    /* ************************ */
    /* * Provider Operations * */
    /* ************************ */

    /**
     * Save a provider entity.
     *
     * @param provider the provider to save
     * @return the saved provider
     */
    @Transactional
    public Provider saveProvider(Provider provider) {
        log.debug("Saving provider: {}", provider);
        return providerRepository.save(provider);
    }

    /**
     * Find provider by walletAddress.
     *
     * @param walletAddress the walletAddress to search for
     * @return a provider with the specified walletAddress
     */
    @Transactional(readOnly = true)
    public Provider findProviderByWalletAddress(String walletAddress) {
        return providerRepository.findByWalletAddress(walletAddress).orElse(null);
    }

    @Transactional
    public void purgeDatabase() {
        log.info("Purging entire database...");
        // Delete reports first (no dependencies)
        benchmarkReportRepository.deleteAll();
        hardwareReportRepository.deleteAll();
        // Delete computers (references components)
        computerRepository.deleteAll();
        // Delete all components
        cpuRepository.deleteAll();
        gpuRepository.deleteAll();
        memoryModuleRepository.deleteAll();
        storageRepository.deleteAll();
        networkDeviceRepository.deleteAll();
        genericComponentRepository.deleteAll();
        // Delete computer attribute types
        computerAttributeTypeRepository.deleteAll();
        // Delete providers
        providerRepository.deleteAll();
        // Delete global properties last
        globalPropertyRepository.deleteAll();
        log.info("Database purge completed");
    }
}
