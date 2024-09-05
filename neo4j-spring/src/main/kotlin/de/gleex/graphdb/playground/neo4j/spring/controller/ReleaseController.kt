package de.gleex.graphdb.playground.neo4j.spring.controller

import de.gleex.graphdb.playground.model.Release
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/release")
class ReleaseController {
    @GetMapping("/all")
    fun allReleases(): Flow<Release> {
        return emptyFlow()
    }
}