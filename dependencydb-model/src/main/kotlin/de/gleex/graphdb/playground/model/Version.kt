package de.gleex.graphdb.playground.model

data class Version private constructor(
    val versionString: String,
    val major: Int,
    val minor: Int,
    val patch: Int
) {
    companion object {
        private val VERSION_REGEX = Regex("(<major>\\d+)(.(<minor>\\d+))?(.(<patch>\\d+))?(<suffix>.*)")
        operator fun invoke(versionString: String): Version {
            val matchResult = VERSION_REGEX.matchEntire(versionString)
            return Version(
                versionString,
                matchResult?.groups?.get("major")?.value?.toIntOrNull() ?: 0,
                matchResult?.groups?.get("minor")?.value?.toIntOrNull() ?: 0,
                matchResult?.groups?.get("patch")?.value?.toIntOrNull() ?: 0
            )
        }
    }
}
