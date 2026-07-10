plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinter)
    application
}

group = "net.chrissearle.huts"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.kotlinx.datetime)
}

application {
    mainClass.set("net.chrissearle.huts.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

detekt {
    buildUponDefaultConfig = true
}
