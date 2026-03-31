plugins {
    `java-library`
    id("com.diffplug.spotless") version "7.2.1"
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("io.avaje:avaje-jsonb-generator:3.5")
    annotationProcessor("io.soabase.record-builder:record-builder-processor:44")
    compileOnly("io.soabase.record-builder:record-builder-core:52")
    implementation("io.avaje:avaje-jsonb:3.5")
    implementation("org.jspecify:jspecify:1.0.0")
    testImplementation("com.google.truth:truth:1.4.4")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.11.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
    sourceCompatibility = JavaVersion.toVersion("23")
    targetCompatibility = JavaVersion.toVersion("23")
}

spotless {
    java {
        target("src/**/*.java")
        importOrder()
        removeUnusedImports()
        palantirJavaFormat().formatJavadoc(true)
        formatAnnotations()
        licenseHeader("// (C) Edward Harman \$YEAR")
    }
}
