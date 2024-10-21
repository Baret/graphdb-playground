package de.gleex.graphdb.playground.model

data class Artifact(
    val groupId: GroupId,
    val artifactId: ArtifactId
) {
    val releasesUrl = "/release/get/${groupId.gId}/${artifactId.aId}"
}
