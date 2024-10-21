package de.gleex.graphdb.playground.model

data class Version private constructor(
    val versionString: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String?
) {
    val isSnapshot: Boolean = suffix.isNullOrEmpty().not()

    companion object {
            /**
         * The Regex to match a valid version.
         */
        val VERSION_REGEX = Regex("(?<major>\\d+)(.(?<minor>\\d+))?(.(?<patch>\\d+))?(?<suffix>.*)")

        /**
         * Creates a new [Version] from the given string. A version needs at least a numerical major version.
         * Optionally it may contain a minor and patch version and a suffix.
         *
         * The format is <major>.<minor>.<patch><suffix>
         *
         * @return A new [Version], if [versionString] contains a parsable version
         * @throws IllegalArgumentException if no version could be extracted from the given string.
         */
        operator fun invoke(versionString: String): Version {
            val matchResult = VERSION_REGEX.matchEntire(versionString)
            requireNotNull(matchResult) {
                "No version detected in given string '$versionString'"
            }
            val suffix: String? = matchResult.groups["suffix"]?.value?.trim().takeIf { it?.isNotEmpty() ?: false }
            return Version(
                versionString,
                matchResult.groups["major"]?.value?.toIntOrNull() ?: 0,
                matchResult.groups["minor"]?.value?.toIntOrNull() ?: 0,
                matchResult.groups["patch"]?.value?.toIntOrNull() ?: 0,
                suffix
            )
        }
    }
}
