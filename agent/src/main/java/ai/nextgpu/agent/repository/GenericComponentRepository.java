package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.DeviceType;
import ai.nextgpu.common.model.GenericComponent;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GenericComponent entity operations.
 * Provides methods to perform CRUD operations on GenericComponent entities.
 */
@Repository
public interface GenericComponentRepository extends BaseComponentRepository<GenericComponent, Long> {

    /**
     * Find generic components by type.
     *
     * @param type the device type to search for
     * @return a list of components with the specified type
     */
    List<GenericComponent> findByType(DeviceType type);

    /**
     * Find generic components by specification key (case-insensitive).
     *
     * @param specificationKey the specification key to search for
     * @return a list of components with the specified specification key
     */
    @Query("SELECT g FROM GenericComponent g WHERE LOWER(g.specificationKey) = LOWER(:specificationKey)")
    List<GenericComponent> findBySpecificationKey(@Param("specificationKey") String specificationKey);

    /**
     * Find generic components by specification value (case-insensitive).
     *
     * @param specificationValue the specification value to search for
     * @return a list of components with the specified specification value
     */
    @Query("SELECT g FROM GenericComponent g WHERE LOWER(g.specificationValue) = LOWER(:specificationValue)")
    List<GenericComponent> findBySpecificationValue(@Param("specificationValue") String specificationValue);

    /**
     * Find generic components by type and specification key.
     *
     * @param type the device type to search for
     * @param specificationKey the specification key to search for
     * @return a list of components with the specified type and specification key
     */
    List<GenericComponent> findByTypeAndSpecificationKey(DeviceType type, String specificationKey);

    @Query("""
           SELECT g
           FROM GenericComponent g
           WHERE g.type = :type
             AND LOWER(g.productIdentifier) = LOWER(:productIdentifier)
           """)
    Optional<GenericComponent> findByTypeAndProductIdentifier(
            @Param("type") DeviceType type,
            @Param("productIdentifier") String productIdentifier
    );

    @Query("""
           SELECT g
           FROM GenericComponent g
           WHERE g.type = :type
             AND LOWER(COALESCE(g.manufacturer, '')) = LOWER(COALESCE(:manufacturer, ''))
             AND LOWER(COALESCE(g.model, '')) = LOWER(COALESCE(:model, ''))
             AND LOWER(COALESCE(g.name, '')) = LOWER(COALESCE(:name, ''))
             AND LOWER(COALESCE(g.specificationKey, '')) = LOWER(COALESCE(:specificationKey, ''))
             AND LOWER(COALESCE(g.specificationValue, '')) = LOWER(COALESCE(:specificationValue, ''))
           """)
    Optional<GenericComponent> findByFingerprint(
            @Param("type") DeviceType type,
            @Param("manufacturer") String manufacturer,
            @Param("model") String model,
            @Param("name") String name,
            @Param("specificationKey") String specificationKey,
            @Param("specificationValue") String specificationValue
    );
}
