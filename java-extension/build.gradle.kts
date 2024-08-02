plugins {
    id("java")
}

group = "org.dfinity"
version = "0.1.0-alpha-SNAPSHOT"

repositories {
    mavenCentral()
}

// this locks all dependencies in gradle.lockfile
// build will fail if dependencies are changed
// to update lockfile after changing dependencies run:
// ./gradlew dependencies --write-locks
dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation("net.portswigger.burp.extensions:montoya-api:2024.7")
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.compileClasspath.get().map { if (it.isDirectory()) it else zipTree(it) })
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Version"] = version
    }
}
