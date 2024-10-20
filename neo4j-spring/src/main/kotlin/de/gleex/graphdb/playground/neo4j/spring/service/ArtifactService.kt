package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.Artifact
import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.neo4j.spring.repositories.ArtifactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Service

@Service
class ArtifactService(private val artifactRepository: ArtifactRepository) {
    fun all(): Flow<Artifact> {
        return artifactRepository.findAll()
            .asFlow()
            .map {
                Artifact(GroupId(it.g), ArtifactId(it.a))
            }
    }
}