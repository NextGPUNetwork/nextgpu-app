package ai.nextgpu.common.report;

import com.fasterxml.jackson.databind.JsonNode;
import ai.nextgpu.common.exception.ComponentException;
import ai.nextgpu.common.model.BaseComponent;
import ai.nextgpu.common.model.Computer;
import ai.nextgpu.common.util.MapAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
    @Convert(converter = MapAttributeConverter.class)
    @Column(name = "anomaly", length = 5000)
    private Map<Collection<BaseComponent>, ComponentException> componentExceptionMap;

    public void exportToHtml(String filename) {

    }

    public void exportToPdf(String filename) {

    }

    public void exportToText(String filename) {

    }

    public void updateExceptionMap(Collection<BaseComponent> component, ComponentException exception){
        // Initialize map if null
        if (componentExceptionMap == null) {
            componentExceptionMap = new HashMap<Collection<BaseComponent>, ComponentException>();
        }
        componentExceptionMap.put(component, exception);
    }

    public boolean hasAnyException() {
        return componentExceptionMap != null && !componentExceptionMap.isEmpty();
    }

    @Override
    public JsonNode asJson() {
        return null;
    }
}
