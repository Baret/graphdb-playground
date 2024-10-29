package de.gleex.graphdb.playground.model

data class Artifact(
    val groupId: GroupId,
    val artifactId: ArtifactId,
    val parent: Artifact? = null,
    val modules: Set<Artifact> = emptySet(),
    val releases: Set<Release> = emptySet()
) {
    val coordinate = ArtifactCoordinate(groupId, artifactId)
}
