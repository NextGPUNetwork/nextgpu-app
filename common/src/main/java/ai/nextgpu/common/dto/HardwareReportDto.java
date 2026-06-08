package ai.nextgpu.common.dto;

import ai.nextgpu.common.report.HardwareReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HardwareReportDto extends BaseReportDto {

    private String computerUuid;

    private String reportContent;

    public static HardwareReportDto toDto(HardwareReport hardwareReport){
        return HardwareReportDto.builder()
                .computerUuid(hardwareReport.getComputerUuid())
                .reportContent(hardwareReport.getComputerUuid())
                .uuid(hardwareReport.getUuid())
                .dateCreated(hardwareReport.getDateCreated())
                .description(hardwareReport.getDescription())
                .elapsedTime(hardwareReport.getElapsedTime())
                .build();
    }
}
