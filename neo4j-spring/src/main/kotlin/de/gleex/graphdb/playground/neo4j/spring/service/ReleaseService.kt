package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.ReleaseRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.DependencyRelationship
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {  }

@Service
class ReleaseService(
    private val releaseRepository: ReleaseRepository,
    private val artifactService: ArtifactService
) {
    suspend fun save(releaseCoordinate: ReleaseCoordinate): Flow<Release> {
        return coroutineScope {
            log.debug { "Finding release by ID $releaseCoordinate" }
            releaseRepository.findById(releaseCoordinate.toString())
                .asFlow()
                .onEmpty {
                    val releaseToSave = with(releaseCoordinate) {
                        Release(groupId, artifactId, version, emptySet())
                    }
                    log.debug { "Release not found. Creating new release $releaseToSave" }
                    emitAll(releaseRepository.save(releaseToSave.toDbEntity()).asFlow())
                }
                .map { savedRelease ->
                    log.debug { "Found or saved release $savedRelease" }
                    savedRelease.also {
                        artifactService.addRelease(
                            ArtifactCoordinate(releaseCoordinate.groupId, releaseCoordinate.artifactId),
                            it
                        )
                    }
                }
                .mapToDomainModel()
        }

    }

    fun findReleasesInGroup(validGroupId: GroupId): Flow<Release> = releaseRepository.findAllByG(validGroupId.gId)
        .asFlow()
        .mapToDomainModel()

    fun all(): Flow<Release> = releaseRepository.findAll()
        .asFlow()
        .mapToDomainModel()

    fun findReleasesOfArtifact(artifact: Artifact): Flow<Release> =
        releaseRepository.findAllByGAndA(artifact.groupId.gId, artifact.artifactId.aId)
            .asFlow()
            .mapToDomainModel()

    private fun Flow<ReleaseEntity>.mapToDomainModel(): Flow<Release> =
        map { it.toDomainModel() }

    private fun ReleaseEntity.toDomainModel(): Release =
        Release(
            groupId = GroupId(g),
            artifactId = ArtifactId(a),
            version = Version(version),
            dependencies = dependencies.map { dbDependency ->
                Dependency(
                    dbDependency.isTransitive,
                    dbDependency.dependsOn.toDomainModel()
                )
            }.toSet()
        )

    private fun Release.toDbEntity(): ReleaseEntity = ReleaseEntity(
        id = null,
        g = groupId.gId,
        a = artifactId.aId,
        version = version.versionString,
        major = version.major,
        minor = version.minor,
        patch = version.patch,
        dependencies = dependencies.map {
            DependencyRelationship(id = null, isTransitive = it.isTransitive, dependsOn = it.release.toDbEntity())
        }.toSet()
    )
}