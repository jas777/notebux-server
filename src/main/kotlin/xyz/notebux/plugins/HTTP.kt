package xyz.notebux.plugins

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureHTTP() {
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        allowCredentials = true
        host("localhost:3000", schemes = listOf("http", "https"), subDomains = listOf("api"))
        host("notebux.xyz", schemes = listOf("http", "https"), subDomains = listOf("api"))
//        anyHost()
    }
}
