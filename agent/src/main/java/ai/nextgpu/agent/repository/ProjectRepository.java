package ai.nextgpu.agent.repository;

import ai.nextgpu.agent.model.Project;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends BaseRepository<Project, Long> {
}
