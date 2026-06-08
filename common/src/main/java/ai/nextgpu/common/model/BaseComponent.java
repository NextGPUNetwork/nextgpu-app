package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.Objects;

/**
 * Abstract class that collects common attributes of Computer components. All component-related classes should inherit this class
 */
@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseComponent extends BaseEntity {

    @Column(length = 255)
    @Comment("Name of manufacturer, e.g. Intel, NVidia, Kingston, Seagate, etc.")
    private String manufacturer;

    @Column(length = 255)
    @Comment("Model number as marketed by the manufacturer, e.g. Gigabyte RTX 4080 Gaming OC")
    private String model;

    @Comment("Year of release helps in identifying end of life of components")
    private Integer yearReleased;

    @Comment("If a component is discontinued, it has a low change to be replaced with the same part.")
    private Boolean isDiscontinued;

    @Comment("Thermal Design Power (TDP) in Watts. All components aggregate to give a fair estimate of total power usage")
    private Integer tdpWatts;

    @Comment("The product ID assigned by the vendor. It may not be globally unique, but should be unique vendor-wise")
    private String productIdentifier;

    @Override
    public String toString() {
        return super.toString() + "; ProductIdentifier: " + productIdentifier +  "; Manufacturer: " + manufacturer + "; Model: " + model;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseComponent that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(manufacturer, that.manufacturer) && Objects.equals(model, that.model) && Objects.equals(yearReleased, that.yearReleased) && Objects.equals(productIdentifier, that.productIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), manufacturer, model, yearReleased, productIdentifier);
    }

    public abstract void compareForAudit(BaseComponent other) throws RuntimeException;

    /**
     * Helper method to compare floating point numbers with percentage tolerance
     *
     * @param value1 first value, which the value of the DB record
     * @param value2 second value
     * @param tolerance maximum allowed percentage difference (e.g., 5.0 for 5%)
     * @return true if the values are within the allowed percentage difference
     */
    protected boolean exceedsTolerance(Integer value1, Integer value2, double tolerance) {
        if (value2 == null) return true;
        if (value1 == null) return false; // The value is coming from the DB record.
        if (value1.equals(value2)) return false;

        double diff = Math.abs(value1 - value2);
        double avg = (value1 + value2) / 2.0;
        double percentDiff = (diff / avg) * 100.0;

        return percentDiff > tolerance;
    }
}
