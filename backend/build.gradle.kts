import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  id("io.micronaut.minimal.application") version "3.7.7"
  id("kotlin-kapt")
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
  mavenCentral()
}

micronaut {
  version.set("3.8.8")
}

// TODO - review this - better way to set versions?
val kotlinVersion = "1.8.10"
val ktormVersion = "3.6.0"
val testContainersVersion = "1.18.0"
val sparkVersion = "3.3.0"

// Exclude unused dependencies to ensure that the backend jar does not exceed
// the AWS Lambda limit of 250MiB uncompressed.
// excludes are declared globally here to avoid repeated declarations across
// the spark dependencies that are used in this module.
configurations {
    all {
      exclude("org.apache.hadoop.shaded")
      exclude("com.google.crypto.tink")
      exclude("com.google.flatbuffers")
      exclude("com.google.protobuf")
      exclude("com.twitter")
      exclude("org.apache.arrow")
      exclude("org.apache.avro")
      exclude("org.apache.curator", "curator-recipes")
      exclude("org.apache.hadoop", "hadoop-client")
      exclude("org.apache.hadoop", "hadoop-client-runtime")
      exclude("org.apache.hive")
      exclude("org.apache.ivy")
      exclude("org.apache.orc")
      exclude("org.apache.parquet")
      exclude("org.rocksdb")
      exclude("org.sparkproject")
      exclude("org.apache.spark", "spark-kvstore_2.12")
      exclude("org.apache.spark", "spark-launcher_2.12")
      exclude("org.apache.spark", "spark-network-shuffle_2.12")
      exclude("org.apache.spark", "spark-sketch_2.12")
      exclude("org.apache.spark", "spark-tags_2.12")
      exclude("org.apache.spark", "spark-unsafe_2.12")
      exclude("org.apache.zookeeper", "zookeeper")
      exclude("org.glassfish.jersey.containers")
      exclude("org.glassfish.jersey.core")
      exclude("org.glassfish.jersey.inject")
      exclude("org.xerial.snappy")
    }
}

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

  implementation("io.micronaut.aws:micronaut-aws-cloudwatch-logging")

  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

  compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("org.ktorm:ktorm-core:$ktormVersion")
  implementation("org.ktorm:ktorm-jackson:$ktormVersion")

  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("io.micronaut.sql:micronaut-jdbc-dbcp")

  implementation("org.postgresql:postgresql:42.6.0")

  // Spark dependencies
  implementation("org.apache.spark:spark-catalyst_2.12:$sparkVersion")
  implementation("org.apache.spark:spark-core_2.12:$sparkVersion")
  implementation("org.apache.spark:spark-sql_2.12:$sparkVersion")

  kapt("io.micronaut:micronaut-inject-java")
  kapt("io.micronaut:micronaut-http-validation")

  kapt("io.micronaut.serde:micronaut-serde-processor:1.5.2")
  runtimeOnly("io.micronaut.serde:micronaut-serde-jackson:1.5.2")
  implementation("io.micronaut.serde:micronaut-serde-api:1.5.2")

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
    minimize()
  }

}
