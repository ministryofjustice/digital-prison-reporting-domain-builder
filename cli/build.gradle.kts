import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  id("org.jetbrains.kotlin.kapt")
  id("org.jetbrains.kotlin.plugin.allopen") version "1.8.10"
  id("io.micronaut.minimal.application") version "3.7.7"
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
  mavenCentral()
}

micronaut {
  version.set("3.8.8")
}

val kotlinVersion = "1.8.10"

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")

  implementation("info.picocli:picocli")
  implementation("info.picocli:picocli-shell-jline3:4.7.1")
  implementation("io.micronaut.picocli:micronaut-picocli")
  implementation("io.micronaut.serde:micronaut-serde-api:1.5.2")
  implementation("io.micronaut:micronaut-jackson-databind")
  implementation("io.micronaut:micronaut-runtime")
  implementation("io.micronaut:micronaut-validation")
  implementation("jakarta.annotation:jakarta.annotation-api")
  implementation("org.fusesource.jansi:jansi:2.4.0")
  implementation(project(":common"))
  implementation("io.micronaut.reactor:micronaut-reactor")

  kapt("io.micronaut.serde:micronaut-serde-processor:1.5.2")
  kapt("io.micronaut:micronaut-inject-java")

  kaptTest("io.micronaut:micronaut-inject-java")

  runtimeOnly("ch.qos.logback:logback-classic")
  runtimeOnly("io.micronaut.serde:micronaut-serde-jackson:1.5.2")

  testImplementation("io.micronaut.test:micronaut-test-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
  testImplementation(testFixtures(project(":common")))
  testImplementation("io.micronaut:micronaut-http-server-netty")
  testImplementation("io.mockk:mockk:1.13.5")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}


application {
  mainClass.set("uk.gov.justice.digital.cli.DomainBuilder")
}

tasks {
  named<Test>("test") {
    useJUnitPlatform()
  }

  named<JavaExec>("run") {
    // Ensure we attach STDIN so interactive mode works when using the run task
    standardInput = System.`in`
  }

  named<ShadowJar>("shadowJar") {
    archiveBaseName.set("domain-builder-cli-frontend")
    destinationDirectory.set(File("${project.rootDir}/build/libs"))
  }

}
