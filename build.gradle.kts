plugins {
    id("com.diffplug.spotless") version "8.5.1" apply false
}

val dockerBuild by tasks.registering(Exec::class) {
    dependsOn(":app:installDist")
    commandLine("docker build -t temperature4:latest .".split(' '))
}
