package ai.nextgpu.common.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.util.Collection;

/**
 * The BaseJson class extends the ObjectNode class to provide enhanced functionality
 * for managing JSON objects. It allows insertion of serializable objects or collections
 * into the JSON structure and provides methods for retrieving elements.
 */
public class BaseJson extends ObjectNode {

    public static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    public BaseJson(JsonNodeFactory nc) {
        super(nc == null ? FACTORY : nc);
    }

    /**
     * Stores the given data object in the JSON structure under the key "data".
     * The provided data object must implement the {@code Serializable} interface.
     *
     * @param data the object to be stored in the JSON structure
     * @throws IllegalArgumentException if the provided data object is not serializable
     */
    public void put(Object data) {
        if (!(data instanceof Serializable)) {
            throw new IllegalArgumentException("Data object must be serializable");
        }
        putPOJO("data", data);
    }

    /**
     * Stores the given collection of objects in the JSON structure under the key "data".
     * Each object in the provided collection must implement the {@code Serializable} interface.
     *
     * @param data the collection of objects to be stored in the JSON structure
     * @throws IllegalArgumentException if the collection is not empty and contains any object
     *         that is not serializable
     */
    public void put(Collection<Object> data) {
        if (data != null && !data.isEmpty() && !(data.iterator().next() instanceof Serializable)) {
            throw new IllegalArgumentException("Data objects must be serializable");
        }
        putPOJO("data", data);
    }

    /**
     * Retrieves the first element from the collection stored under the "data" key in the JSON structure.
     *
     * @return the first object in the collection stored under the "data" key, or {@code null} if the key
     *         does not exist or the collection is empty
     */
    public Object getFirst() {
        return get("data").elements().next();
    }
}
