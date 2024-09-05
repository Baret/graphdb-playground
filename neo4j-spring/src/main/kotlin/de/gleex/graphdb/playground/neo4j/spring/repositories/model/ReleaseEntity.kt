package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.Node

@Node("Release")
data class ReleaseEntity(
    val a: String
)
