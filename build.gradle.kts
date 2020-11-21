import groovy.lang.Closure

plugins {
    kotlin("jvm") version "1.4.20"
    id("org.jetbrains.dokka") version "1.4.10.2"
    id("com.palantir.git-version") version "0.12.3"
    id("org.hibernate.build.maven-repo-auth") version "3.0.4"
    `maven-publish`
}
group = "me.ddevil"
val gitVersion: Closure<String> by project.extra
version = gitVersion()

val kotlinCoroutinesVersion by project.extra("1.4.1")
val bukkitApiVersion by project.extra("1.12.2-R0.1-SNAPSHOT")

repositories {
    jcenter()
    maven {
        name = "spigot-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

dependencies {
    api("org.bukkit:bukkit:$bukkitApiVersion")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.mockito:mockito-all:1.9.5")
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
        // Add opt in compiler option to allow compilation of BukkitDispatcher.kt without warning
        freeCompilerArgs + "opt-in=kotlin.RequiresOptIn"
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

publishing {
    repositories {
        maven {
            name = "lunari"
            val baseUrl = "https://repo.lunari.studio/repository"
            val endPoint = if (version.toString().endsWith("SNAPSHOT")) {
                "maven-snapshots"
            } else {
                "maven-releases"
            }
            url = uri("$baseUrl/$endPoint")
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}