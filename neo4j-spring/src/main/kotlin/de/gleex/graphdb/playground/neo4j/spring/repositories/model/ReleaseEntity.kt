package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.*
import org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING

@Node("Release")
data class ReleaseEntity(
    @Id
    @GeneratedValue(generatorRef = "mavenCoordinateIdGenerator")
    var id: String?,
    val g: String,
    val a: String,
    val version: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
    @Relationship(value = "DEPENDS_ON", direction = OUTGOING)
    val dependencies: Set<DependencyRelationship>
)
