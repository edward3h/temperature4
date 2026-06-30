plugins {
    application
    id("com.diffplug.spotless") version "8.8.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.avaje:avaje-jsonb:3.14")
    testImplementation(project(":kumo"))
    testImplementation("com.google.truth:truth:1.4.5")
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
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "org.ethelred.kumoconfig.KumoConfigMain"
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
