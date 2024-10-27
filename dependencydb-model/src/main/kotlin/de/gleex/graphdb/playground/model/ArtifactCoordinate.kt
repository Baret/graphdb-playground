package de.gleex.graphdb.playground.model

data class ArtifactCoordinate(
    val groupId: GroupId,
    val artifactId: ArtifactId
) {
    override fun toString(): String {
        return "${groupId.gId}:${artifactId.aId}"
    }
}
