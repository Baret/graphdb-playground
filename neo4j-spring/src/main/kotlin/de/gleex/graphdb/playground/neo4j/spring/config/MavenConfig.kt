package de.gleex.graphdb.playground.neo4j.spring.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.nio.file.Path

@ConfigurationProperties("maven")
data class MavenConfig(
    @Validated
    val home: Path,
    @Validated
    val workingDir: Path
)