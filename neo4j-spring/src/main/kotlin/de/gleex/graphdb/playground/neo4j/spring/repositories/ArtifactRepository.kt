package de.gleex.graphdb.playground.neo4j.spring.repositories

import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactEntity
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactWithParent
import org.springframework.data.neo4j.repository.Neo4jRepository

interface ArtifactRepository: Neo4jRepository<ArtifactEntity, String> {
    fun findAllArtifactWithParentsBy(): List<ArtifactWithParent>
    fun findArtifactWithParentsByGAndA(g: String, a: String): ArtifactWithParent
}