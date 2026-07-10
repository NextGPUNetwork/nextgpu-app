package ai.nextgpu.common.repository;

import ai.nextgpu.common.model.BaseComponent;
import ai.nextgpu.common.util.StringUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Base repository for component types that need PostgreSQL-specific query functions (e.g. regexp_replace),
 * kept separate from BaseComponentRepository so that running against other databases aren't coupled to Postgres-only SQL functions.
 */
@NoRepositoryBean
public interface PostgresComponentRepository<T extends BaseComponent, ID extends Serializable>
        extends BaseComponentRepository<T, ID>, JpaSpecificationExecutor<T> {

//    @Query("""
//    SELECT c FROM #{#entityName} c WHERE
//        CASE :alphaNumericOnly WHEN 1 THEN LOWER(cast(regexp_replace(c.model, '[^a-zA-Z0-9]+', '', 'g') as string)) ELSE LOWER(c.model) END =
//        CASE :alphaNumericOnly WHEN 1 THEN LOWER(cast(regexp_replace(:model, '[^a-zA-Z0-9]+', '', 'g') as string)) ELSE LOWER(:model) END
//    """)
    default List<T> findByModelFuzzy(String model, boolean alphaNumericOnly) {
        String target = alphaNumericOnly
                ? model.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase()
                : model.toLowerCase();
        List<T> all = findAll();
        List<T> matching = new ArrayList<>();
        for (T t : all) {
            if (t.getModel() == null) {
                continue;
            }
            String source = alphaNumericOnly
                    ? t.getModel().replaceAll("[^a-zA-Z0-9]+", "").toLowerCase()
                    : t.getModel().toLowerCase();
            if (source.equals(target)) {
                return List.of(t); // exact match found, return immediately
            }
            if (source.contains(target) || target.contains(source)) {
                matching.add(t);
            }
        }
        if (matching.size() == 1) {
            return matching;
        }
        // Another attempt to minimize the number of matches by splitting into tokens and match where most of the tokens exist in arrays (bi-directional match)
        List<T> bestMatches = new ArrayList<>();
        double bestScore = 0; // lower is better for Levenshtein
        for (T t : matching) {
            String source = t.getManufacturer() + " " + t.getModel().toLowerCase();
            // Optional fast prefilter
            double cosine = StringUtil.getCosineSimilarity(source, model);
            if (cosine > bestScore) {
                bestScore = cosine;
                bestMatches.clear();
                bestMatches.add(t);
            } else if (cosine == bestScore) {
                bestMatches.add(t);
            }
        }
        return bestMatches;
    }
}
