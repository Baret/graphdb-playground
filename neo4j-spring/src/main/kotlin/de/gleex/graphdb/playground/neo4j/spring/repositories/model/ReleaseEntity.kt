package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node("Release")
data class ReleaseEntity(
    @Id
    val id: Long,
    val a: String,
    val g: String,
    val version: String,
    val major: Int,
    val minor: Int,
    val patch: Int
)
