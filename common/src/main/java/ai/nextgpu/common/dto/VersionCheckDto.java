package ai.nextgpu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for checking the latest available version of the application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionCheckDto {
    private String version;
    private String downloadUrl;
    private String releaseNotes;
}
