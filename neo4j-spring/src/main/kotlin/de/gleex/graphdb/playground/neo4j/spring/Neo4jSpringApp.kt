package de.gleex.graphdb.playground.neo4j.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = [Neo4jSpringApp::class])
class Neo4jSpringApp

fun main(args: Array<String>) {
    runApplication<Neo4jSpringApp>(*args)
}