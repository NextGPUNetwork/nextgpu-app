package ai.nextgpu.common.dto;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@ToString
public abstract class BaseReportDto {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-hhmmss");

    private String uuid;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    private String description;

    private Long elapsedTime;
}
