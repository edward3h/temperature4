// (C) Edward Harman 2025
package org.ethelred.temperature4.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.avaje.config.Configuration;
import io.avaje.inject.aop.AspectProvider;
import io.avaje.inject.aop.MethodInterceptor;
import jakarta.inject.Singleton;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CacheAspectProvider implements AspectProvider<Cacheable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheAspectProvider.class);

    private final Configuration configuration;
    private final ConcurrentMap<String, Cache<List<Object>, Object>> caches = new ConcurrentHashMap<>();

    public CacheAspectProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public MethodInterceptor interceptor(Method method, Cacheable cacheable) {
        var cache = caches.computeIfAbsent(cacheable.value(), this::buildCache);
        return new CacheableInterceptor(cache);
    }

    private Cache<List<Object>, Object> buildCache(String name) {
        long minutes = configuration.getLong("cache." + name + ".minutes", 1);
        LOGGER.info("buildCache {} with expiry {} minutes", name, minutes);
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(minutes))
                .build();
    }
}
