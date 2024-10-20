package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.Artifact
import de.gleex.graphdb.playground.model.Release
import de.gleex.graphdb.playground.neo4j.spring.service.ArtifactService
import de.gleex.graphdb.playground.neo4j.spring.service.ReleaseService
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/artifact")
class ArtifactController(private val artifactService: ArtifactService) {

    @GetMapping("/get")
    suspend fun allArtifacts(): Flow<Artifact> {
        return artifactService.all()
    }
}