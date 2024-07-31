plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "me.cameronwhyte.duels"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("net.minestom:minestom-snapshots:a1e1dbd8fe")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.cameronwhyte.duels.Main"
    }
}