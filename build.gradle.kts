plugins {
    java
    id("io.quarkus")
    id("com.diffplug.spotless") version "8.10.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-elytron-security-common")
    implementation("io.quarkus:quarkus-smallrye-health")

    // TODO: no SQLite datasource extension currently supports this Quarkus version.
    // io.quarkiverse.jdbc:quarkus-jdbc-sqlite is capped at 3.0.11, built against Quarkus
    // 3.15.1, and has published nothing newer. Postgres is the only working datasource
    // until that extension catches up or an alternative is found.

    implementation("io.quarkiverse.quinoa:quarkus-quinoa:2.9.0")

    // No published Quarkus extension wraps Chicory; used as a plain library, wired up
    // manually in plugins/.
    implementation("com.dylibso.chicory:runtime:1.4.0")
    implementation("com.dylibso.chicory:wasm:1.4.0")
    implementation("com.dylibso.chicory:wasi:1.4.0")

    // MD4 isn't in any standard JDK security provider (deprecated for crypto use, but the ed2k
    // hash anime tooling/AniDB standardized on is built directly on it) — Bouncy Castle is the
    // well-tested implementation rather than hand-rolling one, see library/hash/Ed2kHasher.
    implementation("org.bouncycastle:bcprov-jdk18on:1.85.2")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

group = "de.oppahansi.kosmos"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        formatAnnotations()
    }
}

tasks.named("compileJava") {
    dependsOn("spotlessApply")
}
