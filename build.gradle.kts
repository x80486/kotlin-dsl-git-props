import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
  id("com.github.johnrengelman.shadow").version("5.2.0")
  id("com.gorylenko.gradle-git-properties").version("2.2.2")
  id("org.gradle.application") // https://docs.gradle.org/current/userguide/application_plugin.html
  id("org.gradle.eclipse")
  id("org.gradle.idea")
  id("org.jetbrains.kotlin.jvm").version("1.3.70")
}

// apply(from = "another.gradle.kts")
// apply(plugin = "tld.plugin-identifier")

group = "org.acme"
description = "Container-based Vert.x Application"
version = "0.1.0"

configurations {
  // all { exclude(module = "junit") }
}

repositories {
  // mavenLocal() // Uncomment when needed
  jcenter()
}

dependencies {
  //-----------------------------------------------------------------------------------------------
  //  Experimental Dependencies
  //-----------------------------------------------------------------------------------------------

  //-----------------------------------------------------------------------------------------------
  //  BOM Support
  //-----------------------------------------------------------------------------------------------

  testImplementation(enforcedPlatform("org.junit:junit-bom:5.6.0"))

  //-----------------------------------------------------------------------------------------------
  //  Dependencies
  //-----------------------------------------------------------------------------------------------

  implementation("ch.qos.logback:logback-classic:1.2.3")
  implementation("io.github.microutils:kotlin-logging:1.7.8")
  implementation("io.vertx:vertx-web:${properties["vertx.version"]}")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  //-----------------------------------------------------------------------------------------------
  //  Test Dependencies
  //-----------------------------------------------------------------------------------------------

  testImplementation("io.vertx:vertx-junit5:${properties["vertx.version"]}")
  testImplementation("io.vertx:vertx-web-client:${properties["vertx.version"]}")
  testImplementation("org.assertj:assertj-core:3.15.0")
  testImplementation("org.junit.jupiter:junit-jupiter")
}

//=================================================================================================
//  P L U G I N S
//=================================================================================================

application {
  mainClassName = "io.vertx.core.Launcher"
}

gitProperties {
  extProperty = "gitProps"
  gitPropertiesName = "git.properties"
  keys = listOf("git.branch", "git.build.host", "git.build.version", "git.commit.id", "git.commit.id.abbrev",
      "git.commit.time", "git.remote.origin.url", "git.tags", "git.total.commit.count")
}

//=================================================================================================
//  T A S K S
//=================================================================================================

tasks {
  getByName<JavaExec>("run") {
    args = listOf("run", "org.acme.Application", "--redeploy=${sourceSets.main}",
        "--launcher-class=${application.mainClassName}", "--on-redeploy=${project.projectDir}/gradlew classes")
    jvmArgs = listOf("-Dlogback.configurationFile=${project.projectDir}/src/main/resources/logback.xml",
        "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory")
  }

  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "${JavaVersion.VERSION_11}"
    }
  }

  withType<ShadowJar> {
    val lazyCommitId = object {
      override fun toString(): String {
        println("==> lazyCommitId.toString()")
        val gitProps: Map<String, String> by project.ext
        return gitProps["git.commit.id"] ?: ""
      }
    }
    val lazyValue: String? by lazy {
      println("==> lazyValue")
      val gitProps11: Map<String, String> by project.ext
      gitProps11["git.commit.id"]
    }
    println("==> OK")
    exclude(".gitkeep")
    manifest.attributes.apply {
      put("Application-Name", project.name)
      put("Build-Date", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME))
      // put("Build-Number", )
      put("Created-By", System.getProperty("user.name"))
      put("Gradle-Version", gradle.gradleVersion)
      put("Implementation-Title", project.description)
      put("Implementation-Vendor", "Acme Corp")
      put("Implementation-Version", closureOf<String> { lazyCommitId.toString() })
      // put("Implementation-Version", lambda("git.commit.id"))
      put("JDK-Version", System.getProperty("java.version"))
      put("Main-Verticle", "org.acme.Application")
      put("Specification-Title", project.name)
      put("Specification-Vendor", "Acme Corp")
      put("Specification-Version", project.version)
    }
    println("==> OK")
    mergeServiceFiles { include("META-INF/services/io.vertx.core.spi.VerticleFactory") }
    println("==> OK")
  }

  withType<Test> {
    testLogging {
      events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
      exceptionFormat = TestExceptionFormat.FULL
      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
    useJUnitPlatform()
  }

  //-----------------------------------------------------------------------------------------------
  //  Custom Tasks
  //-----------------------------------------------------------------------------------------------
}
