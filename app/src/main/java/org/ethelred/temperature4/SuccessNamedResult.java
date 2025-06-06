// (C) Edward Harman 2025
package org.ethelred.temperature4;

public record SuccessNamedResult<T>(String name, T value) implements NamedResult<T> {
    @Override
    public boolean success() {
        return true;
    }

    @Override
    public T get() {
        return value;
    }
}
