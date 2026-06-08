package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.Cpu;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Cpu entity operations.
 * Provides methods to perform CRUD operations on Cpu entities.
 */
@Repository
public interface CpuRepository extends BaseComponentRepository<Cpu, Long> {
}
