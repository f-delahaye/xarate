plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
// required to run the 'idea [--no-configuration-cache]' gradle command prior to running the test so that the iml file loaded by TestUtils is present
    id("idea")
}

group = "io.xarate"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1.5")
    type.set("IC") // Target IDE Platform
    plugins.set(listOf("java", "gherkin:231.8109.91", "org.intellij.intelliLang"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }

    buildSearchableOptions {
        enabled = false
    }
    
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    test {
        useJUnitPlatform()
        dependsOn("idea")
        finalizedBy("cleanIdea")
    }	
}

dependencies {
// TestUtils expects karate-core to exist in the iml file so it has to be declared as a dependency (test scope only)
    testImplementation("io.karatelabs:karate-core:1.5.0.RC3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}
