package ai.nextgpu.common.report;

import com.fasterxml.jackson.databind.JsonNode;
import ai.nextgpu.common.exception.ComponentException;
import ai.nextgpu.common.model.BaseComponent;
import ai.nextgpu.common.model.Computer;
import ai.nextgpu.common.util.AnomalyDetailListAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "anomaly_report")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyReport extends BaseReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "anomaly_report_seq_gen")
    @SequenceGenerator(name = "anomaly_report_seq_gen", sequenceName = "anomaly_report_seq", allocationSize = 1)
    @ColumnDefault("nextval('anomaly_report_seq')")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "computer_id")
    @Comment("The computer to generate the report for")
    private Computer computer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = AnomalyDetailListAttributeConverter.class)
    @Column(name = "anomaly")
    private List<AnomalyDetail> anomalyDetails;

    public void exportToHtml(String filename) {
    }

    public void exportToPdf(String filename) {
    }

    public void exportToText(String filename) {
    }

    public void addAnomaly(Collection<BaseComponent> components, ComponentException exception, String message) {
        if (anomalyDetails == null) {
            anomalyDetails = new ArrayList<>();
        }
        AnomalyDetail detail = new AnomalyDetail(
                exception.getErrorCode().name(),
                message,
                components.isEmpty() ? "Unknown" : components.iterator().next().getClass().getSimpleName()
        );
        anomalyDetails.add(detail);
    }

    public boolean hasAnyException() {
        return anomalyDetails != null && !anomalyDetails.isEmpty();
    }

    @Override
    public JsonNode asJson() {
        return null;
    }
}
