package ai.nextgpu.common.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseComponentRepository<T, ID extends Serializable> extends BaseRepository<T, ID> {

    Optional<T> findByName(String name);

    // returns a single entity because of the unique constraint
    @Query("SELECT c FROM #{#entityName} c WHERE LOWER(c.manufacturer) = LOWER(:manufacturer) AND LOWER(c.model) = LOWER(:model)")
    Optional<T> findByManufacturerAndModel(@Param("manufacturer") String manufacturer, @Param("model") String model);

    List<T> findByManufacturer(String manufacturer);

    List<T> findByModel(String model);

}
