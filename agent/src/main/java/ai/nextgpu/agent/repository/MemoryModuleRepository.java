package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.MemoryModule;
import ai.nextgpu.common.model.MemoryType;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for MemoryModule entity operations.
 * Provides methods to perform CRUD operations on MemoryModule entities.
 */
@Repository
public interface MemoryModuleRepository extends BaseComponentRepository<MemoryModule, Long> {
    /**
     * Find memory modules by type.
     *
     * @param type the memory type to search for
     * @return a list of memory modules with the specified type
     */
    List<MemoryModule> findByType(MemoryType type);

    /**
     * Find memory modules by capacity.
     *
     * @param capacity the memory capacity in MB to search for
     * @return a list of memory modules with the specified capacity
     */
    List<MemoryModule> findByCapacity(Integer capacity);
}
