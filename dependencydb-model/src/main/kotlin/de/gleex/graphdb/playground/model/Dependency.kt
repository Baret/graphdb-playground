package de.gleex.graphdb.playground.model

/**
 * The dependency of a release to another release.
 */
data class Dependency(
    val treeDepth: Int,
    val release: ReleaseCoordinate
)