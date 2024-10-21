package de.gleex.graphdb.playground.neo4j.spring.repositories

import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactEntity
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository

interface ArtifactRepository: ReactiveNeo4jRepository<ArtifactEntity, Long> {
}