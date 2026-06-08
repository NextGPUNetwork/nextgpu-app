package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.ComputerAttributeType;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComputerAttributeTypeRepository extends BaseRepository<ComputerAttributeType, Long> {
    Optional<ComputerAttributeType> findByNameIgnoreCase(String name);
}
