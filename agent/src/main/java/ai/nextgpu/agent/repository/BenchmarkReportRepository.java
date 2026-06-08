package ai.nextgpu.agent.repository;

import ai.nextgpu.common.report.BenchmarkReport;
import ai.nextgpu.common.repository.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BenchmarkReportRepository extends BaseRepository<BenchmarkReport, Long> {

}
