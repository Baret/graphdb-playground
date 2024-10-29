package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.*
import org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING

@Node("Artifact")
data class ArtifactEntity(
    @Id
    @GeneratedValue(generatorRef = "mavenCoordinateIdGenerator")
    var id: String?,
    val g: String,
    val a: String,
    @Relationship(type = "HAS_PARENT", direction = OUTGOING, cascadeUpdates = true)
    val parent: ArtifactEntity?,
//    @Relationship(type = "HAS_MODULE", direction = OUTGOING, cascadeUpdates = true)
//    val modules: Set<ArtifactEntity>,
    @Relationship(type = "HAS_RELEASE", direction = OUTGOING, cascadeUpdates = true)
    val releases: Set<ReleaseEntity>,
    @DynamicLabels
    val additionalLabels: Set<String> = if (g.startsWith("de.gleex")) {
        setOf("Internal")
    } else {
        emptySet()
    }
)
