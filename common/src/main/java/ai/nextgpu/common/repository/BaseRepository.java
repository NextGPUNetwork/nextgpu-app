package ai.nextgpu.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.Optional;

public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    default Optional<T> findByUuid(String uuid) {
        return findOneByUuid(uuid);
    }

    Optional<T> findOneByUuid(String uuid);

}
