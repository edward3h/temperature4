// (C) Edward Harman 2025
package org.ethelred.temperature4.cache;

import com.github.benmanes.caffeine.cache.Cache;
import io.avaje.inject.aop.Invocation;
import io.avaje.inject.aop.MethodInterceptor;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class CacheableInterceptor implements MethodInterceptor {
    private final Cache<List<Object>, @Nullable Object> cache;

    public CacheableInterceptor(Cache<List<Object>, @Nullable Object> cache) {
        this.cache = cache;
    }

    @Override
    public void invoke(Invocation invocation) throws Throwable {
        var key = List.of(invocation.arguments());
        invocation.result(cache.get(key, k -> invocation.invokeUnchecked()));
    }
}
