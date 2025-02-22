// (C) Edward Harman 2025
package org.ethelred.temperature4.storage;

import jakarta.inject.Singleton;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Storage {
    private static final Logger LOGGER = LoggerFactory.getLogger(Storage.class);

    private final StorageRoot root;
    private final EmbeddedStorageManager storageManager;

    public Storage() {
        this.root = new StorageRoot();
        this.storageManager = EmbeddedStorage.start(this.root);
        LOGGER.info("Storage loaded {}", this.root);
    }

    public void store() {
        storageManager.storeRoot();
    }

    public void store(Object o) {
        storageManager.store(o);
    }

    public StorageRoot getRoot() {
        return root;
    }
}
