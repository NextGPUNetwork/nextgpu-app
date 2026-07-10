package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.ProviderAttributeType;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProviderAttributeTypeRepository extends BaseRepository<ProviderAttributeType, Long> {

    @Query("SELECT p FROM ProviderAttributeType p WHERE LOWER(p.name) = LOWER(:name)")
    Optional<ProviderAttributeType> findByName(@Param("name") String name);

    @Query("SELECT p FROM ProviderAttributeType p WHERE p.uuid = :uuid")
    Optional<ProviderAttributeType> findByUuid(@Param("uuid") String uuid);

}
