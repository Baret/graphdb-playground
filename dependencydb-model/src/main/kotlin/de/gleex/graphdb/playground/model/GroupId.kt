package de.gleex.graphdb.playground.model

/**
 * A `groupId` of a maven artifact.
 */
@JvmInline
value class GroupId private constructor(val gId: String) {
    companion object {
        /**
         * The regex that may be used to check if a string contains a valid [GroupId].
         */
        val GROUP_ID_REGEX = Regex("[a-zA-z]([a-zA-Z0-9\\-]*.?)+")

        /**
         * Create a valid [GroupId] object from the given string.
         *
         * @throws InvalidGroupIdException when the given string does not match [GROUP_ID_REGEX].
         */
        operator fun invoke(id: String): GroupId {
            if (!GROUP_ID_REGEX.matches(id)) {
                throw InvalidGroupIdException(id)
            }
            return GroupId(id)
        }
    }
}