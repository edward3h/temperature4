// (C) Edward Harman 2024
package org.ethelred.temperature4;

public record ErrorNamedResult<T>(String name, String message) implements NamedResult<T> {
    public ErrorNamedResult(String name, Throwable exception) {
        this(name, exception.getMessage());
    }

    @Override
    public boolean success() {
        return false;
    }

    @Override
    public T get() {
        throw new IllegalStateException();
    }
}
