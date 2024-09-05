package de.gleex.graphdb.playground.model

data class Release(
    val groupId: GroupId,
    val artifactId: ArtifactId,
    val version: Version
) {
    val relatedArtifact: Artifact by lazy { Artifact(groupId, artifactId) }
}
