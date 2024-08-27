package de.gleex.graphdb.playground.model

@JvmInline
value class GroupId private constructor(val gId: String) {
    companion object {
        val GROUP_ID_REGEX = Regex("[a-zA-z]([a-zA-Z0-9\\-]*.?)+")

        operator fun invoke(id: String): GroupId {
            // TODO: validate input
            return GroupId(id)
        }
    }
}