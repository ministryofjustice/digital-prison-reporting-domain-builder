import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("jacoco")
  id("org.sonarqube") version "4.3.1.3277"
  id("org.owasp.dependencycheck") version "8.2.1"
  id("jacoco-report-aggregation")
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

repositories {
  mavenCentral()
}

allprojects {
  apply(plugin = "jacoco")
}

subprojects {
  group = "uk.gov.justice"
  version = if (version != "unspecified") version else "0.0.1-SNAPSHOT"

  jacoco {
      toolVersion = "0.8.8"
  }

  tasks.jacocoTestReport {
      reports {
          xml.required.set(true)
          csv.required.set(false)
          html.required.set(true)
      }
  }

  // when subproject has Jacoco pLugin applied we want to generate XML report for coverage
  plugins.withType<JacocoPlugin> {
      tasks["test"].finalizedBy("jacocoTestReport")
  }

  if (!project.path.startsWith(":bin") && !project.path.startsWith(":cli") && !project.path.startsWith(":docker")) {
      tasks.test {
          useJUnitPlatform()
      }
  }

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

tasks.check {
    dependsOn(tasks.withType(JacocoReport::class))
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

sonarqube {
    properties {
        property("sonar.exclusions", "")
        property("sonar.coverage.exclusions", "")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/jacoco.xml")
        property("sonar.projectKey", "ministryofjustice_digital-prison-reporting-jobs")
        property("sonar.organization", "ministryofjustice")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "DPR :: digital-prison-reporting-jobs")
        property("sonar.core.codeCoveragePlugin", "jacoco")
    }
}