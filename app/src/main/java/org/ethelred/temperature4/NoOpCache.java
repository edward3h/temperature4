// (C) Edward Harman 2025
package org.ethelred.temperature4;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class NoOpCache<K, V> implements Cache<K, V> {
    @Override
    public @Nullable V getIfPresent(K key) {
        return null;
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> mappingFunction) {
        return mappingFunction.apply(key);
    }

    @Override
    public Map<K, @NonNull V> getAllPresent(Iterable<? extends K> keys) {
        return Map.of();
    }

    @Override
    public Map<K, @NonNull V> getAll(
            Iterable<? extends K> keys,
            Function<? super Set<? extends K>, ? extends Map<? extends K, ? extends @NonNull V>> mappingFunction) {
        return Map.of();
    }

    @Override
    public void put(K key, @NonNull V value) {}

    @Override
    public void putAll(Map<? extends K, ? extends @NonNull V> map) {}

    @Override
    public void invalidate(K key) {}

    @Override
    public void invalidateAll(Iterable<? extends K> keys) {}

    @Override
    public void invalidateAll() {}

    @Override
    public long estimatedSize() {
        return 0;
    }

    @Override
    public CacheStats stats() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConcurrentMap<K, @NonNull V> asMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cleanUp() {}

    @Override
    public Policy<K, @NonNull V> policy() {
        throw new UnsupportedOperationException();
    }
}
