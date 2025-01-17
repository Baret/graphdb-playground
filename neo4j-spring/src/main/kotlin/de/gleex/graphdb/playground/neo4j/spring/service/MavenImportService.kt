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

    suspend fun createModulesForArtifact(releaseCoordinate: ReleaseCoordinate): Set<ArtifactCoordinate> {
        return coroutineScope {
            // TODO: only set module if parent matches
            log.info { "Finding and importing modules of release $releaseCoordinate" }
            var savedModules: Set<ArtifactCoordinate> = emptySet()
            val moduleTree: Map<ReleaseCoordinate, Set<ReleaseCoordinate>> = MavenCaller(mavenConfig).resolveModulesRecursively(releaseCoordinate)
            log.info { "Resolved module tree for $releaseCoordinate: $moduleTree" }
            val dbAccess = DirectDatabaseAccess(client)
            moduleTree
                .filterValues { it.isNotEmpty() }
                .map { (parent, modules) ->
                    launch { dbAccess.saveModules(parent, modules) }
                        .invokeOnCompletion {
                            if (parent == releaseCoordinate) {
                                savedModules = modules.map { ArtifactCoordinate(it.groupId, it.artifactId) }.toSet()
                            }
                        }
                }
            savedModules
        }
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
                    launch {
                        saveArtifactModule(child = releaseCoordinate, parent = parent)
                    }
                }
        }
    }

    private suspend fun saveArtifactModule(child: ReleaseCoordinate, parent: ReleaseCoordinate) {
        coroutineScope {
            log.debug { "Trying to detect if $child is a module of $parent to save the artifact's module relation" }
            val mavenCaller = MavenCaller(mavenConfig)
            val moduleArtifactIds = mavenCaller.getModuleArtifactIds(parent)
            if(moduleArtifactIds.contains(child.artifactId)) {
                log.debug { "$child seems to be a module of $parent. Saving artifact HAS_MODULE relation" }
                DirectDatabaseAccess(client).saveArtifactModule(
                    parent = ArtifactCoordinate(
                        parent.groupId,
                        parent.artifactId
                    ),
                    module = ArtifactCoordinate(child.groupId, child.artifactId)
                )
            } else {
                log.debug { "$child does not seem to be a module of $parent. The parent's modules are: ${moduleArtifactIds.joinToString { it.aId }}" }
            }
        }
    }
}
