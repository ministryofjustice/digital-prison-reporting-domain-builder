import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("jacoco")
  id("org.sonarqube") version "4.3.1.3277"
  id("org.owasp.dependencycheck") version "8.2.1"
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
  mavenCentral()
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
    withType<Tar> {
      duplicatesStrategy = DuplicatesStrategy.WARN
    }
    withType<Zip> {
      duplicatesStrategy = DuplicatesStrategy.WARN
    }
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required.set(true)
  }
}

dependencies {
  implementation(project(":common"))
  implementation(project(":backend"))
  implementation(project(":cli"))
}

dependencyCheck {
  suppressionFile = "dependency-check-suppressions.xml"
  failBuildOnCVSS = 4.0F
}
