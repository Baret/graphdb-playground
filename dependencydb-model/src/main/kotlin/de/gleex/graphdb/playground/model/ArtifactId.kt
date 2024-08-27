package de.gleex.graphdb.playground.model

@JvmInline
value class ArtifactId private constructor(val aId: String) {
    companion object {
        val ARTIFACT_ID_REGEX = Regex("[a-zA-z]([a-zA-Z0-9\\-]*.?)+")

        operator fun invoke(id: String): ArtifactId {
            // TODO: validate input
            return ArtifactId(id)
        }
    }
}