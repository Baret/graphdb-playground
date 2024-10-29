package de.gleex.graphdb.playground.neo4j.spring.repositories

import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import org.springframework.data.neo4j.repository.Neo4jRepository

interface ReleaseRepository: Neo4jRepository<ReleaseEntity, String> {
    fun findAllByG(g: String): List<ReleaseEntity>
    fun findAllByGAndA(g: String, a: String): List<ReleaseEntity>
}