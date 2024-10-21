package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING

@Node("Artifact")
data class ArtifactEntity(
    @Id
    @GeneratedValue(generatorRef = "mavenCoordinateIdGenerator")
    var id: String?,
    val g: String,
    val a: String,
    @Relationship(type = "HAS_RELEASE", direction = OUTGOING)
    val releases: Set<ReleaseEntity>
)
