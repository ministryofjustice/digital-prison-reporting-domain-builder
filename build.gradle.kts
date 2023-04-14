import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
}

repositories {
  mavenCentral()
}

subprojects {
  group = "uk.gov.justice"
  version = "1.0-SNAPSHOT"

  tasks {
    withType<KotlinCompile> {
      kotlinOptions.jvmTarget = "11"
    }
  }
}
