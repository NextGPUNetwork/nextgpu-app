package ai.nextgpu.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.Objects;
import java.util.function.BiPredicate;

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

    /**
     * Audits {@code this} persisted component (the expected/DB record) against {@code other}
     * (a freshly reported component of the same type), e.g. from a hardware report, and throws
     * if a specification differs beyond what's allowed.
     * <p>
     * Implementations should first verify {@code other} is the same concrete component type,
     * then compare type-specific fields, using {@link #exceedsTolerance(Integer, Integer, double)}
     * for numeric fields that are allowed to drift within a percentage tolerance.
     *
     * @param other the reported component to compare against this persisted record
     * @throws NullPointerException if {@code other} is {@code null}
     * @throws IllegalArgumentException if {@code other} is not the same component type as {@code this}
     * @throws RuntimeException (typically {@code ComponentException}) if a specification mismatch
     *         or tolerance violation is found
     */
    public abstract void compareForAudit(BaseComponent other) throws RuntimeException;

    /**
     * Compares a standard DB value against an audit value using the supplied condition.
     * <p>
     * {@code standardValue} should be the value from {@code this.<property>}.
     * {@code auditValue} should be the value from {@code other<Component>.<property>}.
     *
     * @param standardValue the expected value from the persisted DB component
     * @param auditValue the actual value from the audit component
     * @param mismatchCondition returns true when the comparison should be treated as a mismatch
     * @return false if the standard DB value is null; otherwise the result of the mismatch condition.
     *         If the audit value is null while the standard value exists, returns true.
     */
    protected <T> boolean comparisonMismatch(T standardValue, T auditValue, BiPredicate<T, T> mismatchCondition) {
        // Returning true if standardValue is null because missing value in dataset should not fail the audit
        if (standardValue == null) return false;
        if (auditValue == null) return true;

        return mismatchCondition.test(standardValue, auditValue);
    }

    /**
     * Helper method to compare floating point numbers with percentage tolerance
     *
     * @param standardValue first value, which the value of the DB record
     * @param auditValue second value
     * @param tolerance maximum allowed percentage difference (e.g., 5.0 for 5%)
     * @return true if the values are within the allowed percentage difference
     */
    protected boolean exceedsTolerance(Integer standardValue, Integer auditValue, double tolerance) {
        // Returning true if standardValue is null because missing value in dataset should not fail the audit
        if (standardValue == null) return false;
        if (auditValue == null) return true; // Audit failure situation
        if (standardValue.equals(auditValue)) return false;
        // For example, the audit system value for CPU l3 cache is greater than the persisted value in dataset
        if(auditValue > standardValue) return false;

        double diff = Math.abs(standardValue - auditValue);
        double avg = (standardValue + auditValue) / 2.0;
        double percentDiff = (diff / avg) * 100.0;

        return percentDiff > tolerance;
    }
}
