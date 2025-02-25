val dockerBuild by tasks.registering(Exec::class) {
    dependsOn(":app:installDist")
    commandLine("docker build -t temperature4:latest .".split(' '))
}
