package ai.nextgpu.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@MappedSuperclass
@ToString
public abstract class BaseMetadata extends BaseObject implements Serializable {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Setter
    @Column(length = 50, unique = true, nullable = false)
    private String name;

    @Setter
    private String datatype;

    @Setter
    private String description;

    @Setter
    private Integer version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Setter
    @LastModifiedDate
    private LocalDateTime dateUpdated;

    @Setter
    private Boolean retired = false;

    @Setter
    private LocalDateTime dateRetired;

    @Setter
    private String retireReason;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseMetadata that)) return false;
        return Objects.equals(getUuid(), that.getUuid()) && Objects.equals(name, that.name)
                && Objects.equals(version, that.version) && Objects.equals(dateCreated, that.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid(), name, version, dateCreated);
    }

    /**
     * Returns an instance of the target class dynamically.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCleanValue(String value) {
        try {
            // Handle primitive types and common Java classes
            Class<?> targetClass = Class.forName(datatype);
            if (targetClass == Integer.class || targetClass == int.class) {
                return (T) targetClass.cast(Integer.parseInt(value));
            }
            if (targetClass == Boolean.class || targetClass == boolean.class) {
                return (T) targetClass.cast(Boolean.parseBoolean(value));
            }
            if (targetClass == Double.class || targetClass == double.class) {
                return (T) targetClass.cast(Double.parseDouble(value));
            }
            if (targetClass == Float.class || targetClass == float.class) {
                return (T) targetClass.cast(Float.parseFloat(value));
            }
            if (targetClass == Long.class || targetClass == long.class) {
                return (T) targetClass.cast(Long.parseLong(value));
            }
            if (targetClass == Short.class || targetClass == short.class) {
                return (T) targetClass.cast(Short.parseShort(value));
            }
            if (targetClass == Byte.class || targetClass == byte.class) {
                return (T) targetClass.cast(Byte.parseByte(value));
            }
            if (targetClass == Character.class || targetClass == char.class) {
                return (T) targetClass.cast(value.charAt(0));
            }
            if (targetClass == String.class) {
                return (T) targetClass.cast(value);
            }

            // Check if the class has a static `fromJson(String json)` method
            try {
                var method = targetClass.getDeclaredMethod("fromJson", String.class);
                return (T) targetClass.cast(method.invoke(null, value));
            } catch (NoSuchMethodException ignored) {
                // If no `fromJson` method exists, fall back to ObjectMapper
            }

            // Convert JSON string to the target class using Jackson
            return (T) objectMapper.readValue(value, targetClass);

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert value: " + value + " into " + datatype, e);
        }
    }
}
