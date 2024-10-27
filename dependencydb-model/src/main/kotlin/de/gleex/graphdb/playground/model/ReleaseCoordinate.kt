package de.gleex.graphdb.playground.model

data class ReleaseCoordinate(
    val groupId: GroupId,
    val artifactId: ArtifactId,
    val version: Version
) {
    override fun toString(): String {
        return "${groupId.gId}:${artifactId.aId}:${version.versionString}"
    }
}
