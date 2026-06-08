package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.Gpu;
import ai.nextgpu.common.model.GpuArchitecture;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Gpu entity operations.
 * Provides methods to perform CRUD operations on Gpu entities.
 */
@Repository
public interface GpuRepository extends BaseComponentRepository<Gpu, Long> {
    List<Gpu> findByArchitecture(GpuArchitecture architecture);
}
