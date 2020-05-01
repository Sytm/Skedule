import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.3.20"
    id("org.jetbrains.dokka") version "0.10.1"
    `maven-publish`
}
group = "com.okkero.skedule"

version = "1.2.7"
val kotlinCoroutinesVersion by project.extra("1.1.0")
val bukkitApiVersion by project.extra("1.12.2-R0.1-SNAPSHOT")

repositories {
    jcenter()
    maven {
        name = "spigot-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

dependencies {
    api("org.bukkit:bukkit:$bukkitApiVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.mockito:mockito-all:1.9.5")
}
val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}
val jar by tasks.getting(Jar::class)

publishing {
    repositories {
        maven("https://repo.lunari.studio/repository/maven-public/") {
            name = "lunari"
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifact(jar)
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}