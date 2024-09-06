package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.model.Release
import de.gleex.graphdb.playground.model.Version
import de.gleex.graphdb.playground.neo4j.spring.repositories.ReleaseRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/release")
class ReleaseController(private val releaseRepository: ReleaseRepository) {

    @GetMapping("/all")
    suspend fun allReleases(): Flux<Release> {
        return releaseRepository.findAll()
//            .asFlow()
            .map {
                Release(
                    GroupId(it.g),
                    ArtifactId(it.a),
                    Version(it.version)
                )
            }
    }
}