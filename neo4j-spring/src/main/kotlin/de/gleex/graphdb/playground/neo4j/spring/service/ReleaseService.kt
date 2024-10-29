package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.ReleaseRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.DependencyRelationship
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class ReleaseService(
    private val releaseRepository: ReleaseRepository,
    private val artifactService: ArtifactService
) {
    suspend fun save(releaseCoordinate: ReleaseCoordinate): Flow<Release> {
        return coroutineScope {
            log.debug { "Finding release by ID $releaseCoordinate" }
            val savedRelease = getOrCreate(releaseCoordinate)
            log.debug { "Found or saved release $savedRelease" }
            launch {
                artifactService.addRelease(
                    ArtifactCoordinate(releaseCoordinate.groupId, releaseCoordinate.artifactId),
                    savedRelease
                )
            }
            flowOf(
                savedRelease.toDomainModel()
            )
        }
    }

    private fun getOrCreate(releaseCoordinate: ReleaseCoordinate): ReleaseEntity =
        releaseRepository.findById(releaseCoordinate.toString())
            .orElseGet {
                val releaseToSave = with(releaseCoordinate) {
                    Release(groupId, artifactId, version, emptySet())
                }
                log.debug { "Release not found. Creating new release $releaseToSave" }
                releaseRepository.save(releaseToSave.toDbEntity())
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
}