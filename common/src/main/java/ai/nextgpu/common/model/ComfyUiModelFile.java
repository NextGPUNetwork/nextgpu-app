package ai.nextgpu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComfyUiModelFile {

    private String url;

    @JsonProperty("repo_id")
    private String repoId;

    @JsonProperty("filename")
    private String fileName;

    @JsonProperty("target_subfolder")
    private String targetSubfolder;
}
