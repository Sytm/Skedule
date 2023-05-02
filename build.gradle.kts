import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.dokka)
    `maven-publish`
}

group = "de.md5lukas"
version = "1.3.0"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    implementation(libs.paper)
    api(libs.coroutines)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.mockito)
}

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.addAll(
        "-Xjvm-default=all",
        "-Xlambdas=indy",
    )
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
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
            name = "md5lukasReposilite"

            url = uri(
                "https://repo.md5lukas.de/${
                    if (version.toString().endsWith("-SNAPSHOT")) {
                        "snapshots"
                    } else {
                        "releases"
                    }
                }"
            )

            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
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
