import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    with (libs.plugins) {
        alias(kotlin)
        alias(spotless)
        alias(dokka)
    }
    `maven-publish`
}

group = "de.md5lukas"
version = "2.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    tasks.withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
          "-Xjvm-default=all",
          "-Xlambdas=indy",
        )
    }
}

dependencies {
    compileOnly(libs.folia)
    runtimeOnly(libs.paper)
    api(libs.coroutines)
    api(project(":schedulers"))
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.mockBukkit)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

spotless {
    kotlin {
        ktfmt()
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
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
