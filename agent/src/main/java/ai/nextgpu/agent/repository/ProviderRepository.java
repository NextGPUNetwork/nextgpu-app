package ai.nextgpu.agent.repository;

import ai.nextgpu.common.model.Cpu;
import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Provider entity operations.
 * Provides methods to perform CRUD operations on Provider entities.
 */
@Repository
public interface ProviderRepository extends BaseRepository<Provider, Long> {

    /**
     * Find a Provider by its walletAddress (case-insensitive).
     *
     * @param walletAddress the walletAddress of the Provider to find
     * @return an Optional containing the Provider if found, or empty if not found
     */
    @Query("SELECT p FROM Provider p WHERE LOWER(p.walletAddress) = LOWER(:walletAddress)")
    Optional<Provider> findByWalletAddress(@Param("walletAddress") String walletAddress);
}
