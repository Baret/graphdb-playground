package de.gleex.graphdb.playground.neo4j.spring.repositories

import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseId
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository

interface ReleaseRepository: ReactiveNeo4jRepository<ReleaseEntity, ReleaseId> {
}