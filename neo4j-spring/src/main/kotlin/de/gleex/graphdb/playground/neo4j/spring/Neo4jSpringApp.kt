package de.gleex.graphdb.playground.neo4j.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Neo4jSpringApp

fun main(args: Array<String>) {
    runApplication<Neo4jSpringApp>(*args)
}