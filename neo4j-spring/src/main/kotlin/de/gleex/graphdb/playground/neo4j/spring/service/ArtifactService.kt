package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.ArtifactRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactEntity
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class ArtifactService(private val artifactRepository: ArtifactRepository) {
    fun all(): Flow<Artifact> {
        return artifactRepository.findAll()
            .asFlow()
            .map {
                log.info { "Artifact ${it.g}:${it.a} has ${it.releases.size} releases" }
                it.releases.forEach { r -> log.info { "\t${r.g}:${r.a}:${r.version}" } }
                Artifact(GroupId(it.g), ArtifactId(it.a))
            }
    }

    private suspend fun createInternal(artifactCoordinate: ArtifactCoordinate): Flow<ArtifactEntity> {
        return artifactRepository.findById(artifactCoordinate.toString())
            .asFlow()
            .onEmpty {
                emitAll(
                    artifactRepository.save(
                        ArtifactEntity(
                            null,
                            artifactCoordinate.groupId.gId,
                            artifactCoordinate.artifactId.aId,
                            emptySet()
                        )
                    )
                        .asFlow()
                )
            }
    }

    suspend fun addRelease(artifactCoordinate: ArtifactCoordinate, release: ReleaseEntity) {
        createInternal(artifactCoordinate)
            .collect { artifactEntity ->
                if (artifactEntity.releases.none { releaseEntity -> releaseEntity.id == release.id }) {
                    val copyWithNewRelease = artifactEntity.copy(releases = artifactEntity.releases + release)
                    artifactRepository.save(copyWithNewRelease)
                }
            }
    }
}