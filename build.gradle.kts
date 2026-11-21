plugins {
    id("com.diffplug.spotless") version "8.12.0" apply false
}

val dockerBuild by tasks.registering(Exec::class) {
    dependsOn(":app:installDist")
    commandLine("docker build -t temperature4:latest .".split(' '))
}
