ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS cpu_benchmark_tool,
    DROP COLUMN IF EXISTS cpu_single_core_score,
    DROP COLUMN IF EXISTS cpu_multi_core_score;

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS gpu_benchmark_tool,
    DROP COLUMN IF EXISTS gpu_openglscore,
    DROP COLUMN IF EXISTS gpu_vulkan_score,
    DROP COLUMN IF EXISTS gpu_float_point_score,
    DROP COLUMN IF EXISTS secondary_gpu_benchmark_tool,
    DROP COLUMN IF EXISTS secondary_gpu_openglscore,
    DROP COLUMN IF EXISTS secondary_gpu_vulkan_score,
    DROP COLUMN IF EXISTS secondary_gpu_float_point_score,
    DROP COLUMN IF EXISTS tertiary_gpu_benchmark_tool,
    DROP COLUMN IF EXISTS tertiary_gpu_openglscore,
    DROP COLUMN IF EXISTS tertiary_gpu_vulkan_score,
    DROP COLUMN IF EXISTS tertiary_gpu_float_point_score;

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS memory_benchmark_tool,
    DROP COLUMN IF EXISTS memory_read_speed,
    DROP COLUMN IF EXISTS memory_write_speed,
    DROP COLUMN IF EXISTS memory_latency;

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS storage_benchmark_tool,
    DROP COLUMN IF EXISTS storage_sequential_read_speed,
    DROP COLUMN IF EXISTS storage_sequential_write_speed,
    DROP COLUMN IF EXISTS storage_random_readiops,
    DROP COLUMN IF EXISTS storage_random_writeiops;

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS network_benchmark_tool,
    DROP COLUMN IF EXISTS network_download_speed,
    DROP COLUMN IF EXISTS network_upload_speed,
    DROP COLUMN IF EXISTS network_latency;

ALTER TABLE benchmark_report
    ADD COLUMN cpu_benchmark_results jsonb,
    ADD COLUMN gpu_benchmark_results jsonb,
    ADD COLUMN memory_benchmark_results jsonb,
    ADD COLUMN storage_benchmark_results jsonb,
    ADD COLUMN network_benchmark_results jsonb,
    ADD COLUMN other_benchmark_results jsonb;

-- Add comments for documentation
COMMENT ON COLUMN benchmark_report.cpu_benchmark_results IS 'CPU benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.gpu_benchmark_results IS 'GPU benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.memory_benchmark_results IS 'Memory benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.storage_benchmark_results IS 'Storage benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.network_benchmark_results IS 'Network benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.other_benchmark_results IS 'Other benchmark metrics stored as JSON';
