package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.neo4j.core.schema.DynamicLabels
import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
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
    val dependencies: Set<DependencyRelationship>,
    @DynamicLabels
    val additionalLabels: Set<String> = if (g.startsWith("de.gleex")) {
        setOf("Internal")
    } else {
        emptySet()
    }
)
