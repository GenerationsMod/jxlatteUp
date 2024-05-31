plugins {
    java
    idea
    `maven-publish`
}

group = "com.traneptora"
version = "2.1.0"

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--add-modules", "jdk.incubator.vector"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--add-modules", "jdk.incubator.vector")
}


tasks.withType<JavaExec> {
    jvmArgs = listOf("--add-modules=jdk.incubator.vector")
}

publishing {
    publications.create<MavenPublication>("mavenCommon").from(components["java"])

    repositories {
        mavenLocal()
        maven {
            val releasesRepoUrl = "https://maven.generations.gg/releases"
            val snapshotsRepoUrl = "https://maven.generations.gg/snapshots"
            url = uri(if (project.version.toString().endsWith("SNAPSHOT") || project.version.toString().startsWith("0")) snapshotsRepoUrl else releasesRepoUrl)
            name = "Generations-Repo"
            credentials {
                username = getGensCredentials().first
                password = getGensCredentials().second
            }
        }
    }
}

private fun getGensCredentials(): Pair<String?, String?> {
    val username = (project.findProperty("gensUsername") ?: System.getenv("GENS_USERNAME") ?: "") as String?
    val password = (project.findProperty("gensPassword") ?: System.getenv("GENS_PASSWORD") ?: "") as String?
    return Pair(username, password)
}
