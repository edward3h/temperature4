// (C) Edward Harman 2025
package org.ethelred.temperature4;

import io.avaje.config.Configuration;
import io.avaje.inject.BeanScope;
import io.avaje.inject.spi.GenericType;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.Plugin;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        BeanScope bs = BeanScope.builder().build();
        var updater = bs.get(SettingUpdater.class);
        updater.start();
        var configuration = bs.get(Configuration.class);
        List<Plugin<Void>> services = bs.list(new GenericType<Plugin<Void>>() {});
        Javalin.create(cfg -> {
                    cfg.router.contextPath = configuration.get("server.contextPath", "/");
                    services.forEach(cfg::registerPlugin);
                    cfg.staticFiles.add("/static", Location.CLASSPATH);
                })
                .beforeMatched(Main::beforeRequest)
                .afterMatched(Main::afterRequest)
                .start(configuration.getInt("server.port", 8080));
    }

    private static void afterRequest(Context context) {
        LOGGER.info("After {} {}", context.method(), context.path());
    }

    private static void beforeRequest(Context context) {
        LOGGER.info("Before {} {}", context.method(), context.path());
    }
}
