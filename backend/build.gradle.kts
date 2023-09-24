import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

apply plugin: 'jacoco'
apply plugin: 'jacoco-report-aggregation'

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.21"
  id("io.micronaut.minimal.application") version "3.7.10"
  id("kotlin-kapt")
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
  mavenCentral()
}

micronaut {
  version.set("3.9.4")
}

val kotlinVersion = "1.8.21"
val ktormVersion = "3.6.0"
val testContainersVersion = "1.18.0"

dependencies {
  implementation(project(":common"))
  testImplementation(testFixtures(project(":common")))

  implementation("io.micronaut.aws:micronaut-function-aws-api-proxy")
  implementation("io.micronaut.flyway:micronaut-flyway")
  implementation("io.micronaut.picocli:micronaut-picocli")
  implementation("io.micronaut:micronaut-http-client")
  implementation("io.micronaut:micronaut-http-server-netty")
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut:micronaut-runtime")
  implementation("io.micronaut:micronaut-validation")

  implementation("io.burt:athena-jdbc:0.4.0")
  implementation("com.amazonaws:aws-java-sdk-dynamodb:1.12.523")

  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

  compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("org.ktorm:ktorm-core:$ktormVersion")
  implementation("org.ktorm:ktorm-jackson:$ktormVersion")

  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("io.micronaut.sql:micronaut-jdbc-dbcp")

  implementation("org.postgresql:postgresql:42.6.0")

  kapt("io.micronaut:micronaut-inject-java")
  kapt("io.micronaut:micronaut-http-validation")

  kapt("io.micronaut.serde:micronaut-serde-processor:1.5.3")
  runtimeOnly("io.micronaut.serde:micronaut-serde-jackson:1.5.3")
  implementation("io.micronaut.serde:micronaut-serde-api:1.5.3")

  kaptTest("io.micronaut:micronaut-inject-java")
  kaptTest("io.micronaut:micronaut-http-validation")

  testImplementation("io.micronaut.test:micronaut-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")

  testImplementation("org.testcontainers:testcontainers-bom:$testContainersVersion")
  testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
  testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
  testImplementation("org.testcontainers:postgresql")

  testImplementation("io.mockk:mockk:1.13.5")
}

application {
  // For local testing only.
  mainClass.set("uk.gov.justice.digital.backend.DomainBuilderBackend")
}

tasks {

  named<Test>("test") {
    useJUnitPlatform()
  }

  named<ShadowJar>("shadowJar") {
    archiveBaseName.set("domain-builder-backend-api")
    destinationDirectory.set(File("${project.rootDir}/build/libs"))
    setProperty("zip64", true)
  }

}