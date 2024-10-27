package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.shared.invoker.*
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension

private val log = KotlinLogging.logger { }

@Service
class PomReadingService(private val config: MavenConfig) {
    private val mavenInvoker: Invoker by lazy {
        DefaultInvoker().apply {
            mavenHome = config.home.toFile()
            workingDirectory = config.workingDir.toFile()
        }
    }

    fun directDependenciesOf(releaseCoordinate: ReleaseCoordinate) : Set<ReleaseCoordinate> {
        val pomPath: Path = locatePomFile(releaseCoordinate)
        val directDependencies: Set<ReleaseCoordinate> = directDependenciesOfPomFile(pomPath)
        return directDependencies
    }

    private fun locatePomFile(releaseCoordinate: ReleaseCoordinate): Path {
        TODO("Not yet implemented")
    }

    private fun directDependenciesOfPomFile(pomPath: Path): Set<ReleaseCoordinate> {
        TODO("Not yet implemented")
    }
}

suspend fun main() {
    val baseDirectory: Path = withContext(Dispatchers.IO) {
        val requestedPath = Path.of("mavenInvokerBaseDir")
        if (requestedPath.exists()) {
            requestedPath
        } else {
            Files.createDirectory(requestedPath)
        }
    }
    log.info { "Using base directory ${baseDirectory.absolutePathString()}" }

    log.info { "Creating invoker" }
    val mavenInvoker: Invoker = DefaultInvoker()

    val pomUri =
        URI.create("https://repo1.maven.org/maven2/de/gleex/kng/kotlin-name-generator/0.1.0/kotlin-name-generator-0.1.0.pom")

    log.info { "Created URI: $pomUri" }

    val localPomFile: Path = baseDirectory.resolve(pomUri.path.substringAfterLast("/"))

    if (localPomFile.exists()) {
        log.info { "Found local file at $localPomFile" }
    } else {
        withContext(Dispatchers.IO) {
            log.info { "Downloading pom from $pomUri to ${localPomFile.absolutePathString()}..." }
            val readBytes = pomUri.toURL().readBytes()
            log.info { "Downloaded ${readBytes.size} bytes" }
            Files.write(localPomFile, readBytes)
            log.info { "Write file ${localPomFile.absolutePathString()}" }
        }
    }

    log.info { "Creating invocation request..." }
    val mavenHome = File("C:\\Program Files\\apache-maven-3.6.3\\")
    log.info { "MAVEN_HOME=$mavenHome" }
    val request: InvocationRequest = DefaultInvocationRequest().apply {
        pomFile = localPomFile.toFile()
        goals = listOf("org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree")
        addArgs(
            listOf(
                "-B",
                "-Dmaven.repo.local=./repo/",
                "-DoutputFile=${localPomFile.nameWithoutExtension}_depTree_verbose.dot",
                "-DoutputType=dot"
            )
        )
        isRecursive = false
        this.mavenHome = mavenHome
        updateSnapshotsPolicy = UpdateSnapshotsPolicy.ALWAYS
    }

    log.info { "Executing request with pomFile=${request.pomFile.absolutePath} goals=${request.goals} mavenHome=${request.mavenHome}" }
    val invocationResult = mavenInvoker.execute(request)

    log.info { "Got result: exitCode=${invocationResult.exitCode}" }
}