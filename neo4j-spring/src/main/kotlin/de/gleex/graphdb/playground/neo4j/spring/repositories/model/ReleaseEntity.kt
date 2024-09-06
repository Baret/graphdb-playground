package de.gleex.graphdb.playground.neo4j.spring.repositories.model

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node("Release")
data class ReleaseEntity(
    @Id
    @GeneratedValue
    var id: Long?,
    val g: String,
    val a: String,
    val version: String,
    val major: Int,
    val minor: Int,
    val patch: Int
)
