package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.Storage;
import ai.nextgpu.common.model.StorageType;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Storage entity operations.
 * Provides methods to perform CRUD operations on Storage entities.
 */
@Repository
public interface StorageRepository extends BaseComponentRepository<Storage, Long> {

    /**
     * Find storage devices by type.
     *
     * @param type the storage type to search for
     * @return a list of storage devices with the specified type
     */
    List<Storage> findByType(StorageType type);

    /**
     * Find storage devices by capacity.
     *
     * @param capacity the storage capacity in GB to search for
     * @return a list of storage devices with the specified capacity
     */
    List<Storage> findByCapacity(Integer capacity);

    /**
     * Find storage devices with capacity greater than or equal to the specified value.
     *
     * @param capacity the minimum storage capacity in GB
     * @return a list of storage devices with capacity greater than or equal to the specified value
     */
    @Query("SELECT s FROM Storage s WHERE s.capacity >= :capacity")
    List<Storage> findByCapacityGreaterThanEqual(@Param("capacity") Integer capacity);

}
