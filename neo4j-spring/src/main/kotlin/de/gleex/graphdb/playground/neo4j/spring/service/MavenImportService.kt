package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.apache.maven.shared.invoker.*
import org.neo4j.driver.summary.ResultSummary
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Service
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
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

    suspend fun import(releaseCoordinate: ReleaseCoordinate): Artifact {
        log.debug { "Starting to import release coordinate $releaseCoordinate" }
        return coroutineScope {
            val savedDependencies: Deferred<List<Dependency>> = async { importDependencies(releaseCoordinate) }
            val parent: Deferred<ArtifactCoordinate> = async { findParentArtifact(releaseCoordinate) }
            val modules: Deferred<Set<ArtifactCoordinate>> = async { findModulesForArtifact(releaseCoordinate) }
            val release = Release(
                groupId = releaseCoordinate.groupId,
                artifactId = releaseCoordinate.artifactId,
                version = releaseCoordinate.version,
                dependencies = savedDependencies.await().toSet()
            )
            val artifact = Artifact(
                groupId = releaseCoordinate.groupId,
                artifactId = releaseCoordinate.artifactId,
                parent = parent.await(),
                modules = modules.await(),
                releases = setOf(release)
            )
            artifact
        }
    }

    private fun findModulesForArtifact(releaseCoordinate: ReleaseCoordinate): Set<ArtifactCoordinate> {
        TODO("Not yet implemented")
    }

    private fun findParentArtifact(releaseCoordinate: ReleaseCoordinate): ArtifactCoordinate {
        TODO("Not yet implemented")
    }

    private suspend fun MavenImportService.importDependencies(releaseCoordinate: ReleaseCoordinate): List<Dependency> {
        val resolvedDependencies: List<Dependency> = withContext(Dispatchers.IO) {
            val pomPath: Path = locatePomFile(releaseCoordinate) ?: return@withContext emptyList()
            resolveDependencies(releaseCoordinate, pomPath)
        }
        val savedDependencies: List<Dependency> = saveToDatabase(releaseCoordinate, resolvedDependencies)
        return savedDependencies
    }

    private suspend fun saveToDatabase(releaseCoordinate: ReleaseCoordinate, dependencies: List<Dependency>): List<Dependency> {
        return coroutineScope {
            val jobs: MutableList<Job> = mutableListOf()
            val artifactsToSave: Channel<ReleaseCoordinate> = Channel()
            launch(Dispatchers.IO) { saveArtifacts(artifactsToSave) }
            val rootRelease: ReleaseCoordinate = saveReleaseAndArtifact(releaseCoordinate, artifactsToSave)
            val savedDependencies: MutableList<Dependency> = Collections.synchronizedList(mutableListOf())
            dependencies.forEach { dep ->
                jobs += launch(Dispatchers.IO) { saveReleaseAndArtifact(dep.release, artifactsToSave) }
                val saveDependencyJob = async(Dispatchers.IO) { saveDependency(rootRelease, dep) }
                jobs += launch {
                    savedDependencies += saveDependencyJob.await()
                }
            }
            log.debug { "Started ${jobs.size} jobs to import ${dependencies.size} dependencies. Awaiting that they finish..." }
            jobs.joinAll()
            log.debug { "All ${jobs.size} jobs done. Closing channel." }
            artifactsToSave.close()
            return@coroutineScope savedDependencies.toList()
        }
    }

    private suspend fun saveDependency(rootRelease: ReleaseCoordinate, dependency: Dependency): Dependency {
        coroutineScope {
            log.debug { "Saving dependency $rootRelease -> $dependency" }
                launch(Dispatchers.IO) {
                    val resultSummary: ResultSummary = client.query {
                        """
                            ${rootRelease.mergeClause("root")}
                            ${dependency.release.mergeClause("dependency")}
                            MERGE (root) -[dep:DEPENDS_ON{ treeDepth:${dependency.treeDepth}, treeParent:'${dependency.treeParent}' }]-> (dependency)
                            RETURN root, dep, dependency
                        """.trimIndent()
                    }
                            .run()
                        log.debug {
                            "Saved dependency $rootRelease -> $dependency in ${
                                resultSummary.resultAvailableAfter(
                                    TimeUnit.MILLISECONDS
                                )
                            } ms. Summary: $resultSummary"
                        }
                        log.debug { "${resultSummary.notifications().size} notifications after saving dependency $dependency:" }
                        resultSummary.notifications().forEach { log.debug { "\t$it" } }
                }
            }
        return dependency
    }

    private suspend fun saveReleaseAndArtifact(
        releaseCoordinate: ReleaseCoordinate,
        artifactsToSave: SendChannel<ReleaseCoordinate>
    ): ReleaseCoordinate {
        coroutineScope {
            log.debug { "Saving release $releaseCoordinate and its artifact" }
            launch { artifactsToSave.send(releaseCoordinate) }
                val resultSummary: ResultSummary = client.query {
                    """
                        ${releaseCoordinate.mergeClause()}
                        RETURN r
                    """.trimIndent()
                }
                    .run()
                log.debug { "Saved release $releaseCoordinate in ${resultSummary.resultAvailableAfter(TimeUnit.MILLISECONDS)} ms. Summary: $resultSummary" }
                log.debug { "${resultSummary.notifications().size} notifications after saving release $releaseCoordinate:" }
                resultSummary.notifications().forEach { log.debug { "\t$it" } }
        }
        return releaseCoordinate
    }

    private suspend fun saveArtifacts(
        artifactsToSaveForReleases: ReceiveChannel<ReleaseCoordinate>
    ) {
        coroutineScope {
            log.debug { "Starting to wait for artifacts to save" }
            for (releaseCoordinate in artifactsToSaveForReleases) {
                launch(Dispatchers.IO) {
                    val artifact = ArtifactCoordinate(releaseCoordinate.groupId, releaseCoordinate.artifactId)
                    log.debug { "Saving artifact $artifact with release $releaseCoordinate" }
                        val resultSummary: ResultSummary = client.query {
                            """
                            MERGE ${artifact.cypherNode}
                            ${releaseCoordinate.mergeClause()}
                            MERGE (a) -[dep:HAS_RELEASE]-> (r)
                            RETURN a, dep, r
                        """.trimIndent()
                        }
                            .run()
                        log.debug { "Saved artifact $releaseCoordinate in ${resultSummary.resultAvailableAfter(TimeUnit.MILLISECONDS)} ms. Summary: $resultSummary" }
                        log.debug { "${resultSummary.notifications().size} notifications after saving release $releaseCoordinate:" }
                        resultSummary.notifications().forEach { log.debug { "\t$it" } }
                }
            }
            log.debug { "Saving artifacts done" }
        }
    }

    private val ArtifactCoordinate.cypherNode
        get() = "(a:Artifact { id:'${this.toString()}', g:'${groupId.gId}', a:'${artifactId.aId}' })"

    private fun ReleaseCoordinate.mergeClause(nodeName: String = "r") =
        """
            MERGE ${getMinimalCypherNode(nodeName)}
                ON CREATE SET ${additionalProperties(nodeName)}
        """

    private fun ReleaseCoordinate.getMinimalCypherNode(nodeName: String = "r") = "($nodeName:Release { " +
            "g:'${groupId.gId}', " +
            "a:'${artifactId.aId}', " +
            "version:'${version.versionString}'" +
            "})"

    private fun ReleaseCoordinate.additionalProperties(nodeName: String = "r") =
        "$nodeName.id = '${this.toString()}', " +
        "$nodeName.major = ${version.major}, " +
        "$nodeName.minor = ${version.minor}, " +
        "$nodeName.patch = ${version.patch}, " +
        "$nodeName.suffix = '${version.suffix}', " +
//        (version.suffix?.let { ", suffix = '$it'" } ?: ", ") +
        "$nodeName.isSnapshot = ${version.isSnapshot}"

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