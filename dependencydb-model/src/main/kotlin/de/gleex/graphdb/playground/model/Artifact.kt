package de.gleex.graphdb.playground.model

data class Artifact(
    val groupId: GroupId,
    val artifactId: ArtifactId,
    val parent: Artifact? = null,
    val modules: Set<Artifact> = emptySet()
)
