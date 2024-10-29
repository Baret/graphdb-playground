package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.Artifact
import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.model.Release
import de.gleex.graphdb.playground.neo4j.spring.service.ArtifactService
import de.gleex.graphdb.playground.neo4j.spring.service.ReleaseService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {  }

@RestController
@RequestMapping("/artifact")
class ArtifactController(private val artifactService: ArtifactService) {

    @GetMapping("/get")
    suspend fun allArtifacts(): Flow<Artifact> {
        return artifactService.all()
    }

    @PostMapping("/createDemo")
    suspend fun createDemoData(): Flow<Artifact> {
        val groupId = GroupId("de.gleex.kng")
        var parent = Artifact(groupId, ArtifactId("kotlin-name-generator-parent"))
        val examples = Artifact(groupId, ArtifactId("kotlin-name-generator-examples"), parent = parent)
        val kng = Artifact(groupId, ArtifactId("kotlin-name-generator"), parent = parent)
        val api = Artifact(groupId, ArtifactId("kotlin-name-generator-api"), parent = parent)

        parent = parent.copy(modules = setOf(api, kng, examples))
        log.debug { "Saving demo data artifact $parent" }
        return artifactService.persist(parent)
    }
}