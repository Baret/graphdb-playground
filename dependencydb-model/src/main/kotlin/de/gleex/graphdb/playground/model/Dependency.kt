package de.gleex.graphdb.playground.model

/**
 * The dependency of a release to another release.
 */
data class Dependency(
    val release: ReleaseCoordinate,
    val treeDepth: Int,
    val treeParent: ReleaseCoordinate,
    val scope: String?
)