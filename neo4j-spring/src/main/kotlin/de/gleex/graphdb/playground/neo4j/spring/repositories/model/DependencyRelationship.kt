package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.RelationshipProperties
import org.springframework.data.neo4j.core.schema.TargetNode
import org.springframework.data.neo4j.core.support.UUIDStringGenerator

@RelationshipProperties
data class DependencyRelationship(
    @Id @GeneratedValue
    var id: Long?,
    val isTransitive: Boolean,
    @TargetNode
    val dependsOn: ReleaseEntity
)
