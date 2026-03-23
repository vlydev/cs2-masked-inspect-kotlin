plugins {
    kotlin("jvm") version "2.0.21"
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "io.github.vlydev"
version = "1.1.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.github.vlydev", "cs2-masked-inspect", version.toString())

    pom {
        name.set("cs2-masked-inspect")
        description.set("Offline encoder/decoder for CS2 masked inspect URLs — pure Kotlin, no dependencies")
        url.set("https://github.com/vlydev/cs2-masked-inspect-kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("vlydev")
                name.set("VlyDev")
                email.set("vladdnepr1989@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/vlydev/cs2-masked-inspect-kotlin")
        }
    }
}
