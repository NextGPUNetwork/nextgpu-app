package ai.nextgpu.agent.repository;

import ai.nextgpu.common.report.HardwareReport;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HardwareReportRepository extends BaseRepository<HardwareReport, Long> {

    // TODO: Find latest hardware report using date created field

}
