package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.ComputerAttributeType;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComputerAttributeTypeRepository extends BaseRepository<ComputerAttributeType, Long> {
    @Query("SELECT c FROM ComputerAttributeType c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<ComputerAttributeType> findByName(@Param("name") String name);
}
