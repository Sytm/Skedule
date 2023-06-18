plugins {
  with(libs.plugins) {
    alias(kotlin)
    alias(dokka)
  }
  `maven-publish`
}

group = rootProject.group

version = "2.0.0"

dependencies {
  api(libs.paper)
  api(libs.coroutines)
  api(project(":schedulers"))
  testImplementation(kotlin("test-junit"))
  testImplementation(libs.mockBukkit)
}

kotlin { jvmToolchain(libs.versions.jvmToolchain.get().toInt()) }

val sourcesJar by
    tasks.creating(Jar::class) {
      archiveClassifier.set("sources")
      from(sourceSets.main.get().allSource)
    }

tasks.dokkaHtml {
  dokkaSourceSets {
    configureEach {
      val majorVersion = libs.versions.paper.get().split('.').let { "${it[0]}.${it[1]}" }
      externalDocumentationLink(
          "https://jd.papermc.io/paper/$majorVersion/",
          "https://jd.papermc.io/paper/$majorVersion/element-list")
      externalDocumentationLink(
          "https://jd.papermc.io/folia/$majorVersion/",
          "https://jd.papermc.io/folia/$majorVersion/element-list")
    }
  }
}

val javadocJar by
    tasks.creating(Jar::class) {
      archiveClassifier.set("javadoc")
      from(tasks.dokkaHtml)
    }

publishing {
  repositories {
    maven {
      name = "md5lukasReposilite"

      url =
          uri(
              "https://repo.md5lukas.de/${
                    if (version.toString().endsWith("-SNAPSHOT")) {
                        "snapshots"
                    } else {
                        "releases"
                    }
                }")

      credentials(PasswordCredentials::class)
      authentication { create<BasicAuthentication>("basic") }
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
