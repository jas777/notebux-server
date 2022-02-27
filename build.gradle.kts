val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposedVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "xyz.notebux"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-gson:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Database
    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.7")
    implementation("com.zaxxer:HikariCP:4.0.3")

    // Mailgun
    implementation("net.sargue:mailgun:1.10.0")
    implementation("org.glassfish.jersey.core:jersey-client:2.35")
    implementation("org.glassfish.jersey.core:jersey-common:2.35")
    implementation("org.glassfish.jersey.media:jersey-media-multipart:2.35")
    implementation("org.glassfish.jersey.inject:jersey-hk2:2.35")
    implementation("javax.activation:activation:1.1.1")

    // Passwords
    implementation("at.favre.lib:bcrypt:0.9.0")

    // Tests
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Main-Class" to "xyz.notebux.ApplicationKt"
        ))
    }
}
