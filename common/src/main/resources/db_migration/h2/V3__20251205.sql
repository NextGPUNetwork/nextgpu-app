ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS (cpu_benchmark_tool, cpu_single_core_score, cpu_multi_core_score);

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS (gpu_benchmark_tool, gpu_openglscore, gpu_vulkan_score, gpu_float_point_score,
        secondary_gpu_benchmark_tool, secondary_gpu_openglscore, secondary_gpu_vulkan_score,
        secondary_gpu_float_point_score, tertiary_gpu_benchmark_tool, tertiary_gpu_openglscore,
        tertiary_gpu_vulkan_score, tertiary_gpu_float_point_score);

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS (memory_benchmark_tool, memory_read_speed, memory_write_speed, memory_latency);

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS (storage_benchmark_tool, storage_sequential_read_speed, storage_sequential_write_speed,
        storage_random_readiops, storage_random_writeiops);

ALTER TABLE benchmark_report
    DROP COLUMN IF EXISTS (network_benchmark_tool, network_download_speed, network_upload_speed, network_latency);

ALTER TABLE benchmark_report
    ADD COLUMN (cpu_benchmark_results JSON, gpu_benchmark_results JSON, memory_benchmark_results JSON,
        storage_benchmark_results JSON, network_benchmark_results JSON, other_benchmark_results JSON);

-- Add comments for documentation
COMMENT ON COLUMN benchmark_report.cpu_benchmark_results IS 'CPU benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.gpu_benchmark_results IS 'GPU benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.memory_benchmark_results IS 'Memory benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.storage_benchmark_results IS 'Storage benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.network_benchmark_results IS 'Network benchmark metrics stored as JSON';
COMMENT ON COLUMN benchmark_report.other_benchmark_results IS 'Other benchmark metrics stored as JSON';

