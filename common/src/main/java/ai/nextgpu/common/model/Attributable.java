package ai.nextgpu.common.model;

public interface Attributable<T> {

    String liquify (T value);

    T solidify();
}
