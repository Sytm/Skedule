import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  with(libs.plugins) {
    alias(kotlin) apply false
    alias(spotless)
  }
}

group = "de.md5lukas"

version = "2.0.1"

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

spotless {
  kotlin {
    target("*/src/*/kotlin/**/*.kt", "**/*.kts")
    ktfmt()
  }
}
