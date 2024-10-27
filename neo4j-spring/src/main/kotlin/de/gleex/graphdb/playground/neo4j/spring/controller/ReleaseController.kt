package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.service.ReleaseService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {  }

@RestController
@RequestMapping("/release")
class ReleaseController(private val releaseService: ReleaseService) {

    @GetMapping("/get")
    suspend fun allReleases(): Flow<Release> {
        return releaseService.all()
    }

    @GetMapping("/get/{groupId}")
    suspend fun releasesByGroupId(@PathVariable(required = true) groupId: String): Flow<Release> {
        log.debug { "Getting release for groupID '$groupId'" }
        val validGroupId = GroupId(groupId)
        return releaseService.findReleasesInGroup(validGroupId)
    }

    @GetMapping("/get/{groupId}/{artifactId}")
    suspend fun releasesOfArtifact(
        @PathVariable(required = true) groupId: String,
        @PathVariable(required = true) artifactId: String
    ): Flow<Release> {
        val validGroupId = GroupId(groupId)
        val validArtifactId = ArtifactId(artifactId)
        val artifact = Artifact(validGroupId, validArtifactId)
        log.debug { "Getting releases for artifact '$artifact'" }
        return releaseService.findReleasesOfArtifact(artifact)
    }

    @PostMapping("/create/{groupId}/{artifactId}/{version}")
    suspend fun saveRelease(
        @PathVariable(required = true) groupId: String,
        @PathVariable(required = true) artifactId: String,
        @PathVariable(required = true) version: String
    ): Flow<Release> {
        log.info { "Creating release model object with groupId=$groupId artifactId=$artifactId version=$version" }
        val validRelease = Release(
            groupId = GroupId(groupId),
            artifactId = ArtifactId(artifactId),
            version = Version(version),
            dependencies = setOf(
                Dependency(
                    isTransitive = false,
                    Release(
                        groupId = GroupId("de.gleex.kng"),
                        artifactId = ArtifactId("kotlin-name-generator-api"),
                        version = Version("0.1.0"),
                        dependencies = emptySet()
                    )
                )
            )
        )
        log.info { "Saving release: $validRelease" }
        return releaseService.save(validRelease)
    }
}