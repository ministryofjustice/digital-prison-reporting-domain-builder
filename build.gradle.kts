import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  jacoco
}

repositories {
  mavenCentral()
}

allprojects {
  apply(plugin = "jacoco")

  tasks.withType<Test>().configureEach {
    finalizedBy(tasks.withType<JacocoReport>()) // report is always generated after tests run
  }
  tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.test) // tests are required to run before generating the report
  }
}

subprojects {
  group = "uk.gov.justice"
  version = if (version != "unspecified") version else "0.0.1-SNAPSHOT"

  tasks {
    // Force Java 11 for this project
    withType<KotlinCompile> {
      kotlinOptions.jvmTarget = "11"
    }
    // Allow tests to run in parallel in each module
    withType<Test>().configureEach {
      maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
    }
  }
}
