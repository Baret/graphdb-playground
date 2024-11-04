package de.gleex.graphdb.playground.neo4j.spring.service

import de.gleex.graphdb.playground.model.*
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ArtifactEntity
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.DependencyRelationship
import de.gleex.graphdb.playground.neo4j.spring.repositories.model.ReleaseEntity


internal fun Artifact.toDbEntity(): ArtifactEntity =
    ArtifactEntity(
        null,
        groupId.gId,
        artifactId.aId,
        parent?.toDbEntity(),
//        modules.map { it.toDbEntity() }.toSet(),
        emptySet()
    )

internal fun ArtifactEntity.toDomainModel(): Artifact =
    Artifact(GroupId(g), ArtifactId(a))



internal fun ReleaseEntity.toDomainModel(): Release =
    Release(
        groupId = GroupId(g),
        artifactId = ArtifactId(a),
        version = Version(version),
        dependencies = dependencies.map { dbDependency ->
            Dependency(
                // TODO: Fix mapping
                dbDependency.dependsOn.let {
                    ReleaseCoordinate(GroupId(it.g), ArtifactId(it.a), Version(it.version))
                },
                0,
                dbDependency.dependsOn.let {
                    ReleaseCoordinate(GroupId(it.g), ArtifactId(it.a), Version(it.version))
                }
            )
        }.toSet()
    )

internal fun Release.toDbEntity(resolveDependency: (ReleaseCoordinate) -> ReleaseEntity): ReleaseEntity = ReleaseEntity(
    id = null,
    g = groupId.gId,
    a = artifactId.aId,
    version = version.versionString,
    major = version.major,
    minor = version.minor,
    patch = version.patch,
    dependencies = dependencies.map {
        DependencyRelationship(id = null, treeDepth = it.treeDepth, dependsOn = resolveDependency(it.release))
    }.toSet()
)