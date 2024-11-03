package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.shared.invoker.*
import org.springframework.data.neo4j.core.Neo4jClient
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
class MavenImportService(private val config: MavenConfig, private val client: Neo4jClient) {
    val repoBasePath: Path = config.workingDir.resolve("repo")
    private val mavenInvoker: Invoker by lazy {
        DefaultInvoker().apply {
            mavenHome = config.home.toFile()
            workingDirectory = config.workingDir.toFile()
            localRepositoryDirectory = repoBasePath.toFile()
        }
    }

    suspend fun import(releaseCoordinate: ReleaseCoordinate) : Set<ReleaseCoordinate> {
        log.debug { "Starting to import release coordinate $releaseCoordinate" }
        val pomPath: Path = locatePomFile(releaseCoordinate)
            ?: return emptySet()
        val directDependencies: Set<ReleaseCoordinate> = dependenciesOf(pomPath)
        return directDependencies + releaseCoordinate
    }

    private fun locatePomFile(releaseCoordinate: ReleaseCoordinate): Path? {
        val artifactPomFile: Path = repoBasePath
            .resolve(releaseCoordinate.groupId.gId.replace('.', File.separatorChar))
            .resolve(releaseCoordinate.artifactId.aId)
            .resolve(releaseCoordinate.version.versionString)
            .resolve("${releaseCoordinate.artifactId.aId}-${releaseCoordinate.version.versionString}.pom")
        if(artifactPomFile.exists()) {
            log.debug { "Found pom file for $releaseCoordinate, no need to invoke maven. Full path: ${artifactPomFile.absolutePathString()}" }
            return artifactPomFile
        }
        log.debug { "Invoking maven to get artifact $releaseCoordinate" }
        val errors: MutableList<String> = mutableListOf()
        val invocationResult = mavenInvoker.execute(DefaultInvocationRequest().apply {
            goals = listOf("$PLUGIN:get")
            addArgs(DEFAULT_ARGS + "-Dartifact=$releaseCoordinate")
            setErrorHandler { errors += it }
        })
        if(invocationResult.exitCode != 0 || errors.isNotEmpty()) {
            log.error { "Getting artifact $releaseCoordinate failed. Maven exited with code ${invocationResult.exitCode}" }
            logErrors(invocationResult, errors)
            return null
        } else {
            if(artifactPomFile.exists()) {
                log.debug { "Downloaded pom file for release $releaseCoordinate to ${artifactPomFile.absolutePathString()}" }
                return artifactPomFile
            } else {
                log.error { "Could not find downloaded pom file at ${artifactPomFile.absolutePathString()}" }
                return null
            }
        }
    }

    private fun dependenciesOf(pomPath: Path): Set<ReleaseCoordinate> {
        log.debug { "Invoking maven to get dependencies for pom file $pomPath" }
        val errors: MutableList<String> = mutableListOf()
        val invocationResult = mavenInvoker.execute(DefaultInvocationRequest().apply {
            goals = listOf("$PLUGIN:tree")
            addArgs(DEFAULT_ARGS)
            addArg("-DoutputEncoding=UTF-8")
            addArg("-Dtokens=whitespace")
            val fParam = "-f ${config.workingDir.relativize(pomPath)}"
            log.debug { "Putting '$fParam'" }
            addArg(fParam)
            addArg("-e")
            addArg("-X")
            setErrorHandler { errors += it }
            setOutputHandler { log.debug { "Got output: $it" } }
        })
        if(invocationResult.exitCode != 0 || errors.isNotEmpty()) {
            log.error { "Getting dependencies for pom file $pomPath failed. Maven exited with code ${invocationResult.exitCode}" }
            logErrors(invocationResult, errors)
            return emptySet()
        } else {
            return emptySet()
        }
    }

    private fun logErrors(
        invocationResult: InvocationResult,
        errors: MutableList<String>
    ) {
        if (invocationResult.executionException != null) {
            log.error { "Invocation exception: ${invocationResult.executionException.message}" }
        }
        if (errors.isNotEmpty()) {
            log.error { "Maven logged ${errors.size} errors:" }
            errors.forEach { log.error { "\t$it" } }
        }
    }

    companion object {
        private val PLUGIN = "org.apache.maven.plugins:maven-dependency-plugin:3.8.1"
        private val DEFAULT_ARGS = listOf(
                "-B",
                "-Dmaven.repo.local=./repo/"
            )
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