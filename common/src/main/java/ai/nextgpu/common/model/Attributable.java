package ai.nextgpu.common.model;

/**
 * Converts a strongly-typed value to and from the flexible {@code String} form used to persist
 * it in a generic attribute map (e.g. {@code Map<ComputerAttributeType, String>} on {@code Computer}).
 *
 * @param <T> the strongly-typed representation of the attribute
 */
public interface Attributable<T> {

    /**
     * Converts the given typed value into its {@code String} representation for storage.
     *
     * @param value the typed value to convert
     * @return the {@code String} form of {@code value}
     */
    String liquify (T value);

    /**
     * Reconstructs the strongly-typed value from its stored {@code String} representation.
     *
     * @return the typed value rebuilt from the stored form
     */
    T solidify();
}
