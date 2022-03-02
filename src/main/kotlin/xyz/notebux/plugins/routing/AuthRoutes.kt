package xyz.notebux.plugins.routing

import io.ktor.server.routing.*
import xyz.notebux.services.AuthService

fun Route.authRoutes() {
    post("auth/login") {
        AuthService.handleLogin(this)
    }

    post("auth/register") {
        AuthService.handleRegister(this)
    }
}