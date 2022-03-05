import groovy.lang.Closure

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
    id("com.palantir.git-version") version "0.12.3"
    id("org.hibernate.build.maven-repo-auth") version "3.0.4"
    `maven-publish`
}
group = "me.ddevil"
val gitVersion: Closure<String> by project.extra
version = gitVersion()

val kotlinCoroutinesVersion by project.extra("1.6.0")
val paperVersion by project.extra("1.18.2-R0.1-SNAPSHOT")

repositories {
    jcenter()
    maven ("https://papermc.io/repo/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.mockito:mockito-all:1.10.19")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "17"
        // Add opt in compiler option to allow compilation of BukkitDispatcher.kt without warning
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
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
