package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.ArtifactRepository
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactWithParent
import de.gleex.graphdb.playground.neo4j.spring.service.ArtifactService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {  }

@RestController
@RequestMapping("/artifact")
class ArtifactController(private val artifactService: ArtifactService, private val repo: ArtifactRepository) {

    @GetMapping("/get")
    suspend fun allArtifacts(): Flow<Artifact> {
        return artifactService.all()
    }

    @GetMapping("/get/withParent")
    suspend fun allWithParent(): List<ArtifactWithParent> {
        return repo.findAllArtifactWithParentsBy()
    }

    @GetMapping("/get/withParent/{groupId}/{artifactId}")
    suspend fun artifactWithParent(
        @PathVariable(required = true) groupId: String,
        @PathVariable(required = true) artifactId: String
    ): ArtifactWithParent? {
        return repo.findArtifactWithParentsByGAndA(groupId, artifactId)
    }

    @PostMapping("/createDemo")
    suspend fun createDemoData(): Flow<Artifact> {
        val groupId = GroupId("de.gleex.kng")
        val aIdParent = ArtifactId("kotlin-name-generator-parent")
        val aIdExamples = ArtifactId("kotlin-name-generator-examples")
        val aIdKng = ArtifactId("kotlin-name-generator")
        val aIdApi = ArtifactId("kotlin-name-generator-api")

        val releases: Array<String> = listOf("0.1.0", "0.1.1", "0.1.2").toTypedArray()

        var parent = Artifact(groupId, aIdParent)

        val examples = releases(Artifact(groupId, aIdExamples, parent = parent), *releases)
        val kng = releases(Artifact(groupId, aIdKng, parent = parent), *releases)
        val api = releases(Artifact(groupId, aIdApi, parent = parent), *releases)

        parent = releases(parent.copy(modules = setOf(api, kng, examples)), *releases)
        log.debug { "Saving demo data artifact $parent" }
        return artifactService.persist(parent)
    }

    private fun releases(artifact: Artifact, vararg versions: String): Artifact {
        return artifact.copy(releases = versions.map {
            Release(
                artifact.groupId,
                artifact.artifactId,
                Version(it),
                emptySet()
            )
        }.toSet())
    }
}