package de.gleex.graphdb.playground.model

data class Release(
    val groupId: GroupId,
    val artifactId: ArtifactId,
    val version: Version,
    val dependencies: Set<Dependency>
) {
    val coordinate = ReleaseCoordinate(groupId, artifactId, version)
    val relatedArtifactCoordinate = ArtifactCoordinate(groupId, artifactId)
}
