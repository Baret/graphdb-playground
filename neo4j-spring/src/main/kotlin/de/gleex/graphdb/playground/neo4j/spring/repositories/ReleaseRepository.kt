package de.gleex.graphdb.playground.neo4j.spring.repositories

import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository
import reactor.core.publisher.Flux

interface ReleaseRepository: ReactiveNeo4jRepository<ReleaseEntity, String> {
    fun findAllByG(g: String): Flux<ReleaseEntity>
    fun findAllByGAndA(g: String, a: String): Flux<ReleaseEntity>
}