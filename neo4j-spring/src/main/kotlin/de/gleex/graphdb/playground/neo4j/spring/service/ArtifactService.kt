package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.ArtifactRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactEntity
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class ArtifactService(private val artifactRepository: ArtifactRepository) {
    fun all(): Flow<Artifact> {
        return artifactRepository.findAll()
            .map {
                log.info { "Artifact ${it.g}:${it.a} has ${it.releases.size} releases" }
                it.releases.forEach { r -> log.info { "\t${r.g}:${r.a}:${r.version}" } }
                it.toDomainModel()
            }
            .asFlow()
    }

    suspend fun persist(artifact: Artifact): Flow<Artifact> {
        return flowOf(
            artifactRepository.save(artifact.toDbEntity())
                .toDomainModel()
        )
    }

    suspend fun addRelease(artifactCoordinate: ArtifactCoordinate, release: ReleaseEntity) {
        log.debug { "Adding release $release to artifact $artifactCoordinate" }
        val artifactEntity = createOrGetInternal(artifactCoordinate)
        log.debug { "Found or created artifact $artifactEntity" }
        if (artifactEntity.releases.none { releaseEntity -> releaseEntity.id == release.id }) {
            val copyWithNewRelease = artifactEntity.copy(releases = artifactEntity.releases + release)
            log.debug { "release not found in release list. Saving copy $copyWithNewRelease" }
            val savedArtifact = artifactRepository.save(copyWithNewRelease)
            log.debug { "Saved artifact with release: $savedArtifact" }
        }
    }

    private suspend fun createOrGetInternal(artifactCoordinate: ArtifactCoordinate): ArtifactEntity {
        log.debug { "createInternal for $artifactCoordinate" }
        return artifactRepository.findById(artifactCoordinate.toString())
            .orElseGet {
                val newArtifactEntity = ArtifactEntity(
                    null,
                    artifactCoordinate.groupId.gId,
                    artifactCoordinate.artifactId.aId,
                    null,
                    emptySet(),
                    emptySet()
                )
                log.debug { "No artifact found. Saving new entity $newArtifactEntity" }
                artifactRepository.save(newArtifactEntity)
            }
    }
}