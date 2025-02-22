// (C) Edward Harman 2024
package org.ethelred.temperature4;

public record ErrorNamedResult<T>(String name, String message) implements NamedResult<T> {
    @Override
    public boolean success() {
        return false;
    }

    @Override
    public T get() {
        throw new IllegalStateException();
    }
}
