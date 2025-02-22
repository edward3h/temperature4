// (C) Edward Harman 2025
package org.ethelred.temperature4;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Util {
    private Util() {}

    public static <K, V> Map<K, V> mapBy(Iterable<V> values, Function<V, K> keyExtractor) {
        return StreamSupport.stream(values.spliterator(), false)
                .collect(Collectors.toMap(keyExtractor, Function.identity(), Util::merge));
    }

    public static <K, V> Mapping<K, V> by(Function<V, K> keyExtractor) {
        return new Mapping<>(keyExtractor);
    }

    @SuppressWarnings("unchecked")
    private static <V> V merge(V a, V b) {
        if (a instanceof Comparable<?> && b.getClass().equals(a.getClass())) {
            return ((Comparable<V>) a).compareTo(b) > 0 ? a : b;
        }
        return a;
    }

    public record Mapping<K, V>(Function<V, K> keyExtractor) {
        public Map<K, V> map(List<V> values) {
            return mapBy(values, keyExtractor);
        }
    }
}
