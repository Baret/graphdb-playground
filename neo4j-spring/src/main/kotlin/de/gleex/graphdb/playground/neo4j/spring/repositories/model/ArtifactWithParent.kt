package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING

interface ArtifactWithParent: ArtifactSummary {
    val parent: ArtifactSummary
}