package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationResult
import org.apache.maven.shared.invoker.Invoker
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

private val log = KotlinLogging.logger { }

/**
 * Responsible to invoke maven and return domain objects.
 */
class MavenCaller(private val config: MavenConfig) {
    val repoBasePath: Path = config.workingDir.resolve("repo")

    private val mavenInvoker: Invoker by lazy {
        DefaultInvoker().apply {
            mavenHome = config.home.toFile()
            workingDirectory = config.workingDir.toFile()
            localRepositoryDirectory = repoBasePath.toFile()
        }
    }

    suspend fun resolveDependenciesOfRelease(releaseCoordinate: ReleaseCoordinate): List<Dependency> {
        return withContext(Dispatchers.IO) {
            val pomPath: Path = locatePomFile(releaseCoordinate) ?: return@withContext emptyList()
            resolveDependencies(releaseCoordinate, pomPath)
        }
    }

    fun parentOf(releaseCoordinate: ReleaseCoordinate): ArtifactCoordinate? {
        TODO("Not yet implemented")
    }

    private suspend fun locatePomFile(releaseCoordinate: ReleaseCoordinate): Path? {
        val artifactPomFile: Path = repoBasePath
            .resolve(releaseCoordinate.groupId.gId.replace('.', File.separatorChar))
            .resolve(releaseCoordinate.artifactId.aId)
            .resolve(releaseCoordinate.version.versionString)
            .resolve("${releaseCoordinate.artifactId.aId}-${releaseCoordinate.version.versionString}.pom")
        if (artifactPomFile.exists()) {
            log.debug { "Found pom file for $releaseCoordinate, no need to invoke maven. Full path: ${artifactPomFile.absolutePathString()}" }
            return artifactPomFile
        }
        return downloadPom(releaseCoordinate, artifactPomFile)
    }

    private suspend fun downloadPom(
        releaseCoordinate: ReleaseCoordinate,
        artifactPomFile: Path
    ): Path? {
        log.debug { "Invoking maven to get artifact $releaseCoordinate" }
        val errors: MutableList<String> = mutableListOf()
        val invocationResult = mavenInvoker.execute(DefaultInvocationRequest().apply {
            goals = listOf("$PLUGIN:get")
            addArgs(DEFAULT_ARGS)
            addArg("-Dartifact=$releaseCoordinate")
            addArg("-Dtransitive=false")
            setErrorHandler { errors += it }
        })
        if (invocationResult.exitCode != 0 || errors.isNotEmpty()) {
            log.error { "Getting artifact $releaseCoordinate failed. Maven exited with code ${invocationResult.exitCode}" }
            logErrors(invocationResult, errors)
            return null
        } else {
            if (artifactPomFile.exists()) {
                log.debug { "Downloaded pom file for release $releaseCoordinate to ${artifactPomFile.absolutePathString()}" }
                return artifactPomFile
            } else {
                log.error { "Could not find downloaded pom file at ${artifactPomFile.absolutePathString()}" }
                return null
            }
        }
    }

    private suspend fun resolveDependencies(releaseCoordinate: ReleaseCoordinate, pomPath: Path): List<Dependency> {
        log.debug { "Invoking maven to get dependencies for pom file $pomPath" }
        val errors: MutableList<String> = mutableListOf()
        val depTreeFileName = "${releaseCoordinate.toString().replace(":", "_")}_depTree.txt"
        val invocationResult = mavenInvoker.execute(DefaultInvocationRequest().apply {
            goals = listOf("$PLUGIN:tree")
            pomFile = pomPath.toFile()
            addArgs(DEFAULT_ARGS)
            addArg("-DoutputEncoding=UTF-8")
            addArg("-Dtokens=whitespace")
            addArg("-DoutputFile=$depTreeFileName")
            setErrorHandler { errors += it }
            setOutputHandler { log.debug { it } }
        }.also {
            log.debug {
                "InvocationRequest: mvn -f ${it.pomFile} ${it.goals.joinToString(" ")} ${
                    it.args.joinToString(" ")
                }"
            }
        })
        if (invocationResult.exitCode != 0 || errors.isNotEmpty()) {
            log.error { "Getting dependencies for pom file $pomPath failed. Maven exited with code ${invocationResult.exitCode}" }
            logErrors(invocationResult, errors)
            return emptyList()
        } else {
            val depTreeFile = pomPath.resolveSibling(depTreeFileName)
            if (!depTreeFile.exists()) {
                log.error { "Could not create dependency tree file at ${depTreeFile.absolutePathString()}" }
                return emptyList()
            }
            val treeFileLines = depTreeFile.toFile().readLines(Charsets.UTF_8)
            // TODO: Check that the first line is "de.gleex.kng:kotlin-name-generator-examples:jar:0.1.0"
//            check(treeFileLines.first() == releaseCoordinate.toString()) {
//                "Dependency tree file ${depTreeFile.absolutePathString()} does not belong to requested release coordinate $releaseCoordinate"
//            }
            val dependencies: MutableList<Dependency> = mutableListOf()
            val lastParentOnDepth: MutableMap<Int, ReleaseCoordinate> = mutableMapOf(0 to releaseCoordinate)
            log.debug { "Starting tree traversal with parent map: $lastParentOnDepth" }
            treeFileLines.drop(1)
                .forEach { line ->
                    var depth = -1
                    var reducingLine = line
                    while (reducingLine.startsWith("   ")) {
                        depth++
                        reducingLine = reducingLine.substring(3)
                    }
                    // each artifact is listed in the form of <gId>:<aId>:jar:<version>:compile
                    val coordinateComponents = reducingLine.split(':')
                    val dependencyCoordinate = ReleaseCoordinate(
                        GroupId(coordinateComponents[0]),
                        ArtifactId(coordinateComponents[1]), Version(coordinateComponents[3])
                    )
                    val treeParent = lastParentOnDepth[depth]
                    checkNotNull(treeParent) {
                        "No parent dependency found in tree at depth ${depth - 1} for $dependencyCoordinate"
                    }
                    lastParentOnDepth[depth + 1] = dependencyCoordinate
                    log.debug { "Put parent on depth $depth to ${lastParentOnDepth[depth]}" }
                    log.debug { "Found parent $treeParent for depth $depth - $dependencyCoordinate" }
                    dependencies += Dependency(
                        dependencyCoordinate,
                        depth,
                        treeParent
                    )
                        .also { log.debug { "Adding dependency $it" } }
                }
            return dependencies
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
            "--show-version",
            "-B",
            "-Dmaven.repo.local=./repo/"
        )
    }
}