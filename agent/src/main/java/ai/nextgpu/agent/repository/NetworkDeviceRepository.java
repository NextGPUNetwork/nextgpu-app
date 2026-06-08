package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.NetworkDevice;
import ai.nextgpu.common.repository.BaseComponentRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NetworkDevice entity operations.
 * Provides methods to perform CRUD operations on NetworkDevice entities.
 */
@Repository
public interface NetworkDeviceRepository extends BaseComponentRepository<NetworkDevice, Long> {

    /**
     * Find network devices by MAC address (case-insensitive).
     *
     * @param macAddress the MAC address to search for
     * @return an Optional containing the network device if found, or empty if not found
     */
    @Query("SELECT n FROM NetworkDevice n WHERE LOWER(n.macAddress) = LOWER(:macAddress)")
    Optional<NetworkDevice> findByMacAddress(@Param("macAddress") String macAddress);

    /**
     * Find network devices by speed.
     *
     * @param speed the network speed in Mbps to search for
     * @return a list of network devices with the specified speed
     */
    List<NetworkDevice> findBySpeed(int speed);

}
