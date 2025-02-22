// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.Plugin;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        BeanScope bs = BeanScope.builder().build();
        var updater = bs.get(SettingUpdater.class);
        updater.start();
        List<Plugin<Void>> services = bs.list(new GenericType<Plugin<Void>>() {});
        Javalin.create(cfg -> {
                    services.forEach(cfg::registerPlugin);
                    cfg.staticFiles.add("/static", Location.CLASSPATH);
                })
                .start();
    }
}
