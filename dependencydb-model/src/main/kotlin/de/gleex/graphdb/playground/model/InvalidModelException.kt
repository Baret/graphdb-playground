package de.gleex.graphdb.playground.model

sealed class InvalidModelException(
    override val message: String?
) :
    Throwable(message)

class InvalidGroupIdException(givenGroupId: String): InvalidModelException("Invalid groupId: '$givenGroupId'")

class InvalidArtifactIdException(givenArtifactId: String): InvalidModelException("Invalid artifactId: '$givenArtifactId'")