package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.DynamicLabels
import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

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
    val patch: Int
) {
    @DynamicLabels
    val additionalLabels: Set<String> = if (g.startsWith("de.gleex")) {
        setOf("Internal")
    } else {
        emptySet()
    }
}
