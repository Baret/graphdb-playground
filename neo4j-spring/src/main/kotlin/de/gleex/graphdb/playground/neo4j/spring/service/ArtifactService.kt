package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.Artifact
import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.neo4j.spring.repositories.ArtifactRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {  }

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
}