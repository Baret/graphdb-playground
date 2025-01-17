package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.ArtifactCoordinate
import de.gleex.graphdb.playground.model.Dependency
import de.gleex.graphdb.playground.model.ReleaseCoordinate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.neo4j.driver.summary.ResultSummary
import org.springframework.data.neo4j.core.Neo4jClient
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.cos

private val log = KotlinLogging.logger {  }

class DirectDatabaseAccess(private val client: Neo4jClient) {

    suspend fun saveDependenciesToDatabase(releaseCoordinate: ReleaseCoordinate, dependencies: List<Dependency>): List<Dependency> {
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

    suspend fun saveArtifactAndRelease(releaseCoordinate: ReleaseCoordinate) {
        coroutineScope {
            launch(Dispatchers.IO) {
                val artifact = ArtifactCoordinate(releaseCoordinate.groupId, releaseCoordinate.artifactId)
                log.debug { "Saving artifact $artifact with release $releaseCoordinate" }
                val resultSummary: ResultSummary = client.query {
                    """
                        MERGE ${artifact.getCypherNode()}
                        ${releaseCoordinate.mergeClause()}
                        MERGE (a) -[dep:HAS_RELEASE]-> (r)
                        RETURN a, dep, r
                    """.trimIndent()
                }
                    .run()
                log.debug { "Saved artifact with release $releaseCoordinate in ${resultSummary.resultAvailableAfter(TimeUnit.MILLISECONDS)} ms. Summary: $resultSummary" }
                log.debug { "${resultSummary.notifications().size} notifications after saving artifact with release $releaseCoordinate:" }
                resultSummary.notifications().forEach { log.debug { "\t$it" } }
            }
        }
    }

    suspend fun saveReleaseWithParent(child: ReleaseCoordinate, parent: ReleaseCoordinate) {
        coroutineScope {
            val artifactsToSave: Channel<ReleaseCoordinate> = Channel()
            launch(Dispatchers.IO) { saveArtifacts(artifactsToSave) }
            log.debug { "Saving parent $parent with child $child" }
            artifactsToSave.send(child)
            artifactsToSave.send(parent)
            launch(Dispatchers.IO) {
                val resultSummary: ResultSummary = client.query {
                    """
                        ${parent.mergeClause("p")}
                        ${child.mergeClause("c")}
                        MERGE (c) -[m: HAS_PARENT]-> (p)
                        RETURN c, m, p
                    """.trimIndent()
                }
                    .run()
                log.debug { "Saved parent $parent with child $child in ${resultSummary.resultAvailableAfter(TimeUnit.MILLISECONDS)} ms. Summary: $resultSummary" }
                log.debug { "${resultSummary.notifications().size} notifications after saving parent $parent with child $child:" }
                resultSummary.notifications().forEach { log.debug { "\t$it" } }
            }
            artifactsToSave.close()
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
                        MERGE (root) -[dep:DEPENDS_ON{ treeDepth:${dependency.treeDepth}, treeParent:'${dependency.treeParent}'${dependency.scope?.let { ", scope:'${it}'" } ?: ""} }]-> (dependency)
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

    suspend fun saveArtifactModule(parent: ArtifactCoordinate, module: ArtifactCoordinate) {
        log.debug { "Saving module $module for artifact $parent" }
        coroutineScope {
            launch(Dispatchers.IO) {
                val resultSummary: ResultSummary = client.query {
                    """
                        MERGE ${parent.getCypherNode("p")}
                        MERGE ${module.getCypherNode("m")}
                        MERGE (p) -[dep:HAS_MODULE]-> (m)
                        RETURN p, dep, m
                    """.trimIndent()
                }
                    .run()
                log.debug { "Saved module $module with its parent $parent in ${resultSummary.resultAvailableAfter(TimeUnit.MILLISECONDS)} ms. Summary: $resultSummary" }
            }
        }
    }

    private suspend fun saveReleaseAndArtifact(
        releaseCoordinate: ReleaseCoordinate,
        artifactsToSave: SendChannel<ReleaseCoordinate>
    ): ReleaseCoordinate {
        coroutineScope {
            log.debug { "Saving release $releaseCoordinate and its artifact" }
            launch { artifactsToSave.send(releaseCoordinate) }
            launch(Dispatchers.IO) {
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
        }
        return releaseCoordinate
    }

    private suspend fun saveArtifacts(
        artifactsToSaveForReleases: ReceiveChannel<ReleaseCoordinate>
    ) {
        coroutineScope {
            log.debug { "Starting to wait for artifacts to save" }
            for (releaseCoordinate in artifactsToSaveForReleases) {
                saveArtifactAndRelease(releaseCoordinate)
            }
            log.debug { "Saving artifacts done" }
        }
    }

    private fun ArtifactCoordinate.getCypherNode(nodeName: String = "a") =
        "($nodeName:Artifact { id:'${this.toString()}', g:'${groupId.gId}', a:'${artifactId.aId}' })"

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
        "$nodeName.isSnapshot = ${version.isSnapshot}"

    fun saveModules(parent: ReleaseCoordinate, modules: Set<ReleaseCoordinate>) {
        log.info { "WOULD NOW SAVE ${modules.size} modules for release $parent" }
    }
}
