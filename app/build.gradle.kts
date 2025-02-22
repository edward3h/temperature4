plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    id("gg.jte.gradle") version "3.1.16"
    id("com.diffplug.spotless") version "7.0.2"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    annotationProcessor("io.avaje:avaje-inject-generator:11.2")
    annotationProcessor("io.avaje:avaje-http-javalin-generator:3.0")
    annotationProcessor("io.avaje:avaje-http-client-generator:3.0")
    annotationProcessor("io.avaje:avaje-jsonb-generator:3.0")
    annotationProcessor("io.soabase.record-builder:record-builder-processor:44")
    compileOnly("io.soabase.record-builder:record-builder-core:44")
    implementation("io.avaje:avaje-inject:11.2")
    implementation("io.avaje:avaje-http-api:3.0")
    implementation("io.avaje:avaje-http-client:3.0")
//    implementation("io.avaje:avaje-jex:3.0-RC20")
    implementation("io.javalin:javalin:6.4.0")
    implementation("io.avaje:avaje-jsonb:3.0")
    implementation("gg.jte:jte:3.1.16")
    implementation("gg.jte:jte-models:3.1.16")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("io.avaje:avaje-config:4.0")
    implementation("org.eclipse.store:storage-embedded:2.1.2")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
    jteGenerate("gg.jte:jte-models:3.1.16")
    runtimeOnly("org.logevents:logevents:0.5.7")
    testImplementation("com.google.truth:truth:1.4.4")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.11.1")
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
    sourceCompatibility = JavaVersion.toVersion("23")
    targetCompatibility = JavaVersion.toVersion("23")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview", "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED")
}

application {
    // Define the main class for the application.
    mainClass = "org.ethelred.temperature4.Main"
}

jte {
    sourceDirectory = file("src/main/jte").toPath()
    generate()
    packageName = "org.ethelred.temperature4.template"
    jteExtension("gg.jte.models.generator.ModelExtension")
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