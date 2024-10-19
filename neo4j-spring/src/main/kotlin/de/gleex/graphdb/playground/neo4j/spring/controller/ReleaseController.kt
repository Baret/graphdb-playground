package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.model.Release
import de.gleex.graphdb.playground.model.Version
import de.gleex.graphdb.playground.neo4j.spring.service.ReleaseService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {  }

@RestController
@RequestMapping("/release")
class ReleaseController(private val releaseService: ReleaseService) {

    @GetMapping("/get")
    suspend fun allReleases(): Flow<Release> {
        return releaseService.findAll()
            .asFlow()
            .map {
                Release(
                    GroupId(it.g),
                    ArtifactId(it.a),
                    Version(it.version)
                )
            }
    }

    @GetMapping("/get/{groupId}")
    suspend fun releasesByGroupId(@PathVariable groupId: String): Flow<Release> {
        log.debug { "Getting release for groupID '$groupId'" }
        val validGroupId = GroupId(groupId)
        return releaseService.findAllByG(validGroupId.gId)
            .asFlow()
            .map {
                Release(
                    GroupId(it.g),
                    ArtifactId(it.a),
                    Version(it.version)
                )
            }
    }

    @PostMapping("/create/{groupId}/{artifactId}/{version}")
    suspend fun saveRelease(groupId: String, artifactId: String, version: String): Mono<Release> {
        log.info { "Creating release model object with groupId=$groupId artifactId=$artifactId version=$version" }
        val validRelease = Release(
            GroupId(groupId),
            ArtifactId(artifactId),
            Version(version)
        )
        return releaseService.save(validRelease)
    }
}