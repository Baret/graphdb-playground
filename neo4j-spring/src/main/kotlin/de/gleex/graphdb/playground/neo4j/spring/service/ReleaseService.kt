package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.model.Release
import de.gleex.graphdb.playground.model.Version
import de.gleex.graphdb.playground.neo4j.spring.repositories.ReleaseRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ReleaseService(private val releaseRepository: ReleaseRepository) {
    fun save(validRelease: Release): Mono<Release> {
        return releaseRepository.save(
            ReleaseEntity(
                id = null,
                g = validRelease.groupId.gId,
                a = validRelease.artifactId.aId,
                version = validRelease.version.versionString,
                major = validRelease.version.major,
                minor = validRelease.version.minor,
                patch = validRelease.version.patch
            )
        ).map {
            Release(
                GroupId(it.g),
                ArtifactId(it.a),
                Version(it.version)
            )
        }
    }
}