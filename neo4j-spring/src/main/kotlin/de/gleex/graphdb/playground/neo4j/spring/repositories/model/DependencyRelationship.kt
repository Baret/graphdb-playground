package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.RelationshipProperties
import org.springframework.data.neo4j.core.schema.TargetNode

@RelationshipProperties
data class DependencyRelationship(
    @Id
    @GeneratedValue
    // this MUST be a Long, also the log spams warnings: https://github.com/spring-projects/spring-data-neo4j/issues/2620
    var id: Long?,
    val isTransitive: Boolean,
    @TargetNode
    val dependsOn: ReleaseEntity
)
