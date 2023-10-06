plugins {
    id("java")
}

group = "org.dfinity"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.portswigger.burp.extensions:montoya-api:2023.10.3")
    implementation("net.java.dev.jna:jna:5.13.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    from(configurations.compileClasspath.get().map { if (it.isDirectory()) it else zipTree(it) })
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Version"] = version
    }
}
