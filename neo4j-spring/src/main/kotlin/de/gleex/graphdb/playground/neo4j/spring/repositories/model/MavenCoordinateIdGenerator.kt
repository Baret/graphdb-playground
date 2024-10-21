package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.IdGenerator
import org.springframework.stereotype.Component

/**
 * Generates maven coordinates as IDs for Artifact and Release entities. Throws an exception for all other labels.
 *
 * The format of a maven coordinate is `groupId:artifactId:jar:1.2.3:compile`
 * For ArtifactEntity we simply use the prefix of the full coordinate: `groupId:artifactId`
 */
@Component
class MavenCoordinateIdGenerator: IdGenerator<String> {
    override fun generateId(primaryLabel: String, entity: Any): String {
        return when(primaryLabel) {
            "Artifact" -> {
                val artifactEntity = entity as ArtifactEntity
                "${artifactEntity.g}:${artifactEntity.a}"
            }
            "Release" -> {
                val releaseEntity = entity as ReleaseEntity
                "${releaseEntity.g}:${releaseEntity.a}:jar:${releaseEntity.version}:compile"
            }
            else -> throw IllegalStateException("Can not generate ID for primaryLabel=$primaryLabel and entity=$entity")
        }
    }
}