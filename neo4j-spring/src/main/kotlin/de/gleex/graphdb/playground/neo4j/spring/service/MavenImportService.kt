package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class MavenImportService(private val mavenConfig: MavenConfig, private val client: Neo4jClient) {

    suspend fun import(releaseCoordinate: ReleaseCoordinate): Artifact {
        log.debug { "Starting to import release coordinate $releaseCoordinate" }
        return coroutineScope {
            requireNotNull(MavenCaller(mavenConfig).locatePomFile(releaseCoordinate)) {
                "$releaseCoordinate does not seem to be a valid maven release"
            }
            val savedDependencies: Deferred<List<Dependency>> = async { importDependencies(releaseCoordinate) }
            val parent: Deferred<ReleaseCoordinate?> = async { createParent(releaseCoordinate) }
            val modules: Deferred<Set<ArtifactCoordinate>> = async { createModulesForArtifact(releaseCoordinate) }
            val release = Release(
                groupId = releaseCoordinate.groupId,
                artifactId = releaseCoordinate.artifactId,
                version = releaseCoordinate.version,
                dependencies = savedDependencies.await().toSet()
            )
            val artifact = Artifact(
                groupId = releaseCoordinate.groupId,
                artifactId = releaseCoordinate.artifactId,
                // TODO: artifacts dont have parents
                parent = parent.await()?.let { ArtifactCoordinate(it.groupId, it.artifactId) },
                modules = modules.await(),
                releases = setOf(release)
            )
            artifact
        }
    }

    private suspend fun importDependencies(releaseCoordinate: ReleaseCoordinate): List<Dependency> {
        val mavenCaller = MavenCaller(mavenConfig)
        val databaseCaller = DirectDatabaseAccess(client)
        val resolvedDependencies: List<Dependency> = mavenCaller.resolveDependenciesOfRelease(releaseCoordinate)
        val savedDependencies: List<Dependency> = databaseCaller.saveDependenciesToDatabase(releaseCoordinate, resolvedDependencies)
        return savedDependencies
    }

    private fun createModulesForArtifact(releaseCoordinate: ReleaseCoordinate): Set<ArtifactCoordinate> {
        log.warn { "CREATING MODULES NOT YET IMPLEMENTED (got release $releaseCoordinate)" }
        return emptySet()
    }

    private suspend fun createParent(releaseCoordinate: ReleaseCoordinate): ReleaseCoordinate? {
        return coroutineScope {
            val mavenCaller = MavenCaller(mavenConfig)
            val databaseCaller = DirectDatabaseAccess(client)

            return@coroutineScope mavenCaller.parentOf(releaseCoordinate)
                ?.also { parent ->
                    launch {
                        databaseCaller.saveReleaseWithParent(
                            child = releaseCoordinate,
                            parent = parent
                        )
                    }
                }
        }
    }
}
