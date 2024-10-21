package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.ReleaseRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReleaseService(private val releaseRepository: ReleaseRepository) {
    fun save(validRelease: Release): Flow<Release> = releaseRepository.save(
        ReleaseEntity(
            id = null,
            g = validRelease.groupId.gId,
            a = validRelease.artifactId.aId,
            version = validRelease.version.versionString,
            major = validRelease.version.major,
            minor = validRelease.version.minor,
            patch = validRelease.version.patch
        )
    ).asFlow()
        .mapToDomainModel()

    fun findReleasesInGroup(validGroupId: GroupId): Flow<Release> = releaseRepository.findAllByG(validGroupId.gId)
        .asFlow()
        .mapToDomainModel()

    fun all(): Flow<Release> = releaseRepository.findAll()
        .asFlow()
        .mapToDomainModel()

    fun findReleasesOfArtifact(artifact: Artifact): Flow<Release> = releaseRepository.findAllByGAndA(artifact.groupId.gId, artifact.artifactId.aId)
        .asFlow()
        .mapToDomainModel()

    private fun Flow<ReleaseEntity>.mapToDomainModel(): Flow<Release> =
        map {
            Release(
                GroupId(it.g),
                ArtifactId(it.a),
                Version(it.version)
            )
        }
}