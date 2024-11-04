package de.gleex.graphdb.playground.model

data class Artifact(
    val groupId: GroupId,
    val artifactId: ArtifactId,
    val parent: ArtifactCoordinate? = null,
    val modules: Set<ArtifactCoordinate> = emptySet(),
    val releases: Set<Release> = emptySet()
) {
    val coordinate = ArtifactCoordinate(groupId, artifactId)
}
