package de.gleex.graphdb.playground.neo4j.spring.repositories

import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactEntity
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository

interface ArtifactRepository: Neo4jRepository<ArtifactEntity, String> {
}