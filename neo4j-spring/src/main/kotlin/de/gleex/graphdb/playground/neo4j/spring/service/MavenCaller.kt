package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
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

    suspend fun parentOf(releaseCoordinate: ReleaseCoordinate): ReleaseCoordinate? {
        val pomPath = (locatePomFile(releaseCoordinate)
            ?: return null)
        return coroutineScope {
            log.info { "Invoking maven to detect parent of $releaseCoordinate" }
            val relevantLineRegex = Regex("Ancestor\\sPOMs:\\s(?<groupId>\\S+):(?<artifactId>\\S+):(?<version>\\S+)")
            var detectedParent: ReleaseCoordinate? = null
            val invocationSuccessful = executeMaven("find parent of $releaseCoordinate") {
                pomFile = pomPath.toFile()
                goals = listOf("$DEPENDENCY_PLUGIN:display-ancestors")
                isRecursive = false
                setOutputHandler { line ->
                    log.debug { "[parentDetection] $line" }
                    val matchResult = relevantLineRegex.find(line)
                    val gIdMatch = matchResult?.groups?.get("groupId")
                    val aIdMatch = matchResult?.groups?.get("artifactId")
                    val versionMatch = matchResult?.groups?.get("version")
                    if (gIdMatch != null && aIdMatch != null && versionMatch != null) {
                        log.debug { "\tFound parent in line: $line" }
                        detectedParent = ReleaseCoordinate(
                            GroupId(gIdMatch.value),
                            ArtifactId(aIdMatch.value),
                            Version(versionMatch.value)
                        )
                        log.info { "Parent of $releaseCoordinate is $detectedParent" }
                    }
                }
            }
            if(!invocationSuccessful) {
                log.info { "No parent found for $releaseCoordinate" }
                return@coroutineScope null
            }
            return@coroutineScope detectedParent
        }
    }

    /**
     * Finds all modules of the given [ReleaseCoordinate] and the modules of these and so on.
     *
     * @return a map of parent to list of modules
     */
    suspend fun resolveModulesRecursively(releaseCoordinate: ReleaseCoordinate): Map<ReleaseCoordinate, Set<ReleaseCoordinate>> {
        return coroutineScope {
            log.info { "Resolving modules of $releaseCoordinate" }
            val moduleTree: MutableMap<ReleaseCoordinate, MutableSet<ReleaseCoordinate>> = mutableMapOf(releaseCoordinate to mutableSetOf())
            val pomPath = locatePomFile(releaseCoordinate) ?: return@coroutineScope moduleTree
            // mvn -Dexec.executable='echo' -Dexec.args='${project.parent.groupId}:${project.parent.artifactId}:${project.parent.version} -> ${project.groupId}:${project.artifactId}:${project.version}' exec:exec -q
            executeMaven("resolve modules of $releaseCoordinate") {
                pomFile = pomPath.toFile()
                goals = listOf("$EXEC_PLUGIN:exec")
                isQuiet = true
                isBatchMode = false
                addArg(ARG_REPO)
                addArgs(listOf(
                    "-Dexec.executable='echo'",
                    "-Dexec.args='\${project.parent.groupId}:\${project.parent.artifactId}:\${project.parent.version} -> \${project.groupId}:\${project.artifactId}:\${project.version}'"
                ))
                setOutputHandler {
                    log.debug { "[resolveModules] $it" }
                    val parentAndModule = it.split(" -> ")
                    val parentStrings = parentAndModule[0].split(":")
                    val parent = ReleaseCoordinate(GroupId(parentStrings[0]), ArtifactId(parentStrings[1]), Version(parentStrings[2]))
                    val moduleStrings = parentAndModule[1].split(":")
                    val module = ReleaseCoordinate(GroupId(moduleStrings[0]), ArtifactId(moduleStrings[1]), Version(moduleStrings[2]))
                    log.debug { "Registering module. Parent $parent -> module $module" }
                    moduleTree.merge(parent, mutableSetOf(module)) { set, newSet -> (set + newSet).toMutableSet()}
                    log.debug { "Modules of $parent: ${moduleTree[parent]?.joinToString()}" }
                }
            }
            log.info { "Resolved modules of $releaseCoordinate" }
            log.info { "$releaseCoordinate has ${moduleTree[releaseCoordinate]?.size?:0} modules. Found ${moduleTree.keys.size} module parents total and ${moduleTree.values.sumOf { it.size }} modules total" }
            moduleTree
        }
    }

    /**
     * Returns the [Path] to the pom file of the given release. If this method returns `null`, maven
     * was not able to locate the given release. Mosty possibly the given [ReleaseCoordinate] does not
     * locate a valid maven release.
     */
    suspend fun locatePomFile(releaseCoordinate: ReleaseCoordinate): Path? {
        val artifactPomFile: Path = repoBasePath
            .resolve(releaseCoordinate.groupId.gId.replace('.', File.separatorChar))
            .resolve(releaseCoordinate.artifactId.aId)
            .resolve(releaseCoordinate.version.versionString)
            .resolve("${releaseCoordinate.artifactId.aId}-${releaseCoordinate.version.versionString}.pom")
        if (artifactPomFile.exists()) {
            log.info { "Found pom file for $releaseCoordinate, no need to invoke maven. Full path: ${artifactPomFile.absolutePathString()}" }
            return artifactPomFile
        }
        return downloadPom(releaseCoordinate, artifactPomFile)
    }

    private suspend fun downloadPom(
        releaseCoordinate: ReleaseCoordinate,
        artifactPomFile: Path
    ): Path? {
        log.info { "Invoking maven to get artifact $releaseCoordinate" }
        val invocationSuccessful = executeMaven("get pom file for $releaseCoordinate") {
            goals = listOf("$DEPENDENCY_PLUGIN:get")
            addArgs(DEFAULT_ARGS)
            addArg("-Dartifact=$releaseCoordinate")
            addArg("-Dtransitive=false")
            setOutputHandler { log.debug { "[mvnGet] $it" } }
        }
        if (invocationSuccessful.not()) {
            return null
        } else {
            if (artifactPomFile.exists()) {
                log.info { "Downloaded pom file for release $releaseCoordinate to ${artifactPomFile.absolutePathString()}" }
                return artifactPomFile
            } else {
                log.error { "Could not find downloaded pom file at ${artifactPomFile.absolutePathString()}" }
                return null
            }
        }
    }

    private suspend fun resolveDependencies(releaseCoordinate: ReleaseCoordinate, pomPath: Path): List<Dependency> {
        val depTreeFileName = "${releaseCoordinate.toString().replace(":", "_")}_depTree.txt"
        val depTreeFile = pomPath.resolveSibling(depTreeFileName)
        if (depTreeFile.toFile().exists()) {
            log.debug { "Found existing dependency tree file ${depTreeFile.absolutePathString()} for release $releaseCoordinate" }
        } else {
            log.info { "Invoking maven to get dependencies for pom file $pomPath" }
            val invocationSuccessful = executeMaven("get dependencies for file $pomPath") {
                goals = listOf("$DEPENDENCY_PLUGIN:tree")
                pomFile = pomPath.toFile()
                isRecursive = false
                addArgs(DEFAULT_ARGS)
                addArg("-DoutputEncoding=UTF-8")
                addArg("-Dtokens=whitespace")
                addArg("-DoutputFile=$depTreeFileName")
                setOutputHandler { log.debug { "[depTree] $it" } }
            }
            if (invocationSuccessful.not()) {
                return emptyList()
            }
        }

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
                    treeParent,
                    coordinateComponents[4]
                )
                    .also { log.debug { "Adding dependency $it" } }
            }
        return dependencies
            .also { log.info { "Found ${it.size} dependencies for release $releaseCoordinate" } }
    }

    private suspend fun executeMaven(
        requestedAction: String,
        executionConfig: DefaultInvocationRequest.() -> Unit
    ): Boolean {
        return coroutineScope {
            val errors: MutableList<String> = mutableListOf()
            val mavenJob = async(Dispatchers.IO) {
                mavenInvoker.execute(
                    DefaultInvocationRequest()
                        .apply { setErrorHandler { errors += it } }
                        .apply(executionConfig))
            }
            val invocationResult = mavenJob.await()
            if (invocationResult.exitCode != 0 || errors.isNotEmpty()) {
                log.error { "Invoking maven to '$requestedAction' failed. Maven exited with code ${invocationResult.exitCode}" }
                logErrors(invocationResult, errors)
                return@coroutineScope false
            } else {
                return@coroutineScope true
            }
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
        private const val DEPENDENCY_PLUGIN = "org.apache.maven.plugins:maven-dependency-plugin:3.8.1"
        private const val EXEC_PLUGIN = "org.codehaus.mojo:exec-maven-plugin:3.5.0"

        private const val ARG_REPO = "-Dmaven.repo.local=./repo/"
        private const val ARG_SHOW_VERSION = "--show-version"
        private const val ARG_BATCH_MODE = "-B"

        private val DEFAULT_ARGS = listOf(
            ARG_SHOW_VERSION,
            ARG_BATCH_MODE,
            ARG_REPO
        )
    }
}