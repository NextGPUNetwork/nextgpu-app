package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalPropertyRepository extends BaseRepository<GlobalProperty, Long> {

    /**
     * Find a GlobalProperty by its name with case-insensitive exact matching.
     *
     * @param name The name of the property to search for
     * @return An Optional containing the GlobalProperty if found, empty otherwise
     */
    @Query("SELECT g FROM GlobalProperty g WHERE LOWER(g.name) = LOWER(:name)")
    Optional<GlobalProperty> findByName(@Param("name") String name);
}
