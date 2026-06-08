package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.Computer;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComputerRepository extends BaseRepository<Computer, Long> {

    @Query("SELECT c FROM Computer c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Computer> findByName(@Param("name") String name);

    List<Computer> findByProvider_Id(Long id);
}
