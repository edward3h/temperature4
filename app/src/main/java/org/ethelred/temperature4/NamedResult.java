// (C) Edward Harman 2024
package org.ethelred.temperature4;

public interface NamedResult<T> {
    String name();

    boolean success();

    T get();
}
