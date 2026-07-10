package ai.nextgpu.common.util;

import ai.nextgpu.common.model.ComputerAttributeType;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Field;
import java.util.*;

public class BeanUtils {

    /**
     * Identifies the property names of an object's fields that have null values.
     *
     * @param source the object whose null-valued property names are to be fetched
     * @return an array of property names that are null in the given object
     */
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames.toArray(new String[0]);
    }

    /**
     * Converts a map with keys of type {@code ComputerAttributeType} to a map with
     * string keys, where each key is the result of {@code getName()} on the original key.
     *
     * @param attributes a map where keys are {@code ComputerAttributeType} objects and values are strings.
     *                   If {@code attributes} is null, this method returns null.
     * @return a new map with string keys obtained from the {@code getName()} method of the original keys
     *         and the same corresponding values as in the input map, or null if the input map is null.
     */
    public static Map<String, String> toStringKeyMap(Map<ComputerAttributeType, String> attributes){
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> stringKeyMap = new HashMap<>();
        attributes.forEach((key, value) -> {
            stringKeyMap.put(key.getName(), value);
        });

        return stringKeyMap;
    }

    /**
     * Implements an entity matching algorithm that:
     *     - Get all field upto `superclass` parameter that specifies how far up the inheritance hierarchy to collect fields
     *     - Compares entities field by field to find the best match with given probe
     *     - Scores matches based on the number of exactly matching non-null fields
     *     - Returns the entity with the highest score
     * @param entity1
     * @param entity2
     * @param probe
     * @return
     * @param <T>
     */
    public static <T> int compareMatches(T entity1, T entity2, T probe, Class<?> uptoSuperClass) {
        long score1 = 0;
        long score2 = 0;

        try {
            // Get fields up to the specified superclass
            List<Field> allFields = getAllFields(probe.getClass(), uptoSuperClass);

            for (Field field : allFields) {
                field.setAccessible(true);
                Object probeValue = field.get(probe);

                if (probeValue != null) {
                    Object value1 = field.get(entity1);
                    Object value2 = field.get(entity2);

                    if (probeValue.equals(value1)) {
                        score1++;
                    }
                    if (probeValue.equals(value2)) {
                        score2++;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return Long.compare(score1, score2);
    }

    private static List<Field> getAllFields(Class<?> type, Class<?> upToClass) {
        // Get fields of the current class
        List<Field> fields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));

        // Get fields of parent classes until we reach the specified class or Object
        Class<?> superClass = type.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class) && !superClass.equals(upToClass)) {
            fields.addAll(getAllFields(superClass, upToClass));
        }

        return fields;
    }
}
