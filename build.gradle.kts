import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  jacoco
  id("org.sonarqube") version "3.5.0.2730"
  id("org.owasp.dependencycheck") version "8.2.1"
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
    withType<Test>().configureEach {
      // Allow tests to run in parallel in each module
      maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1
      // Temporarily force verbose test output to diagnose issues on circle ci
      testLogging {
        events = setOf(
          TestLogEvent.FAILED,
          TestLogEvent.PASSED,
          TestLogEvent.SKIPPED,
          TestLogEvent.STANDARD_ERROR,
          TestLogEvent.STANDARD_OUT,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
      }
    }
  }
}
