package de.gleex.graphdb.playground.neo4j.spring.web

import de.gleex.graphdb.playground.model.ArtifactId
import de.gleex.graphdb.playground.model.GroupId
import de.gleex.graphdb.playground.model.ReleaseCoordinate
import de.gleex.graphdb.playground.model.Version
import de.gleex.graphdb.playground.neo4j.spring.config.MavenConfig
import de.gleex.graphdb.playground.neo4j.spring.service.MavenCaller
import de.gleex.graphdb.playground.neo4j.spring.service.MavenImportService
import io.github.allangomes.kotlinwind.css.kw
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.stream.createHTML
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.w3c.dom.Document

@Controller
@RequestMapping("/import")
class ImportFrontendController(private val config: MavenConfig) {
    @GetMapping("/start")
    @ResponseBody
    suspend fun startImport(@RequestParam groupId: String?, @RequestParam artifactId: String?, @RequestParam version: String?): String {
        if (groupId.isNullOrBlank() || artifactId.isNullOrBlank() || version.isNullOrBlank()) {
            return requestAllParameters(groupId, artifactId, version)
        }
        val releaseToImport = ReleaseCoordinate(GroupId(groupId), ArtifactId(artifactId), Version(version))
        MavenCaller(config).locatePomFile(releaseToImport)
            ?: return notAValidRelease(releaseToImport)
        return createHTML().html {
            head {
                title { +"Import to dependency DB" }
            }
            body {
                div{
                    style = kw.inline { flex.col.items_center.justify_start.gap[4] }
                    h1 { +"Import $groupId into the dependency DB" }
                    div {
                        p { +"Here comes a form..." }
                    }
                }
            }
        }
    }

    private fun notAValidRelease(releaseToImport: ReleaseCoordinate): String {
        return createHTML().html {
            head {
                title { +"Invalid maven coordinate" }
            }
            body {
                div {
                    style = kw.inline { flex.col.items_center.justify_start.gap[4] }
                    h2 {
                        +"Invalid maven coordinate"
                    }
                    p {
                        +"The given coordinates "
                        b {
                            style = kw.inline { text.color["red"] }
                            +releaseToImport.toString()
                        }
                        +" do not locate a valid maven release"
                    }
                }
            }
        }
    }

    private fun requestAllParameters(groupId: String?, artifactId: String?, version: String?): String {
        return createHTML().html {
            head {
                title { +"Missing parameters" }
            }
            body {
                div {
                    style = kw.inline { flex.col.items_center.justify_start.gap[4] }
                    h2 {
                        +"Parameters missing"
                    }
                    p {
                        +"Please provide all parameters to start an import"
                    }
                    ul {
                        li {
                            +(groupId?.let { "groupId = $it" } ?: "groupId is missing")
                        }
                        li {
                            +(artifactId?.let { "artifactId = $it" } ?: "artifactId is missing")
                        }
                        li {
                            +(version?.let { "version = $it" } ?: "version is missing")
                        }
                    }

                }
            }
        }
    }
}