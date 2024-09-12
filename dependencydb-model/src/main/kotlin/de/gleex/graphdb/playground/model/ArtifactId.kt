package de.gleex.graphdb.playground.model

/**
 * An `artifactId` of a maven artifact.
 */
@JvmInline
value class ArtifactId private constructor(val aId: String) {
    companion object {
        /**
         * The regex that may be used to check if a string contains a valid [ArtifactId].
         */
        val ARTIFACT_ID_REGEX = Regex("[a-zA-z]([a-zA-Z0-9\\-_]*.?)+")

        /**
         * Create a valid [ArtifactId] object from the given string.
         *
         * @throws InvalidArtifactIdException when the given string does not match [ARTIFACT_ID_REGEX].
         */
        operator fun invoke(id: String): ArtifactId {
            if(!id.matches(ARTIFACT_ID_REGEX)) {
                throw InvalidArtifactIdException(id)
            }
            return ArtifactId(id)
        }
    }
}