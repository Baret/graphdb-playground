package de.gleex.graphdb.playground.neo4j.spring.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

private val log = KotlinLogging.logger {  }

@ConfigurationProperties("maven")
data class MavenConfig(
    val home: Path,
    val workingDir: Path
) {
    @PostConstruct
    fun validateConfig() {
        require(home.isDirectory() and home.exists() and home.isReadable()) {
            "Can not read maven home $home"
        }
        val createdWorkingDir = Files.createDirectories(workingDir.resolve("repo"))
        require(createdWorkingDir.isDirectory() and createdWorkingDir.exists() and createdWorkingDir.isWritable()) {
            "Maven working directory not usable. Configured path: '$workingDir', resolved path: $createdWorkingDir"
        }
        log.info { "Using maven config $this" }
    }
}