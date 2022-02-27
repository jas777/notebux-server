package xyz.notebux.plugins

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.notebux.entities.Users
import xyz.notebux.services.AuthService
import xyz.notebux.services.NoteService
import xyz.notebux.services.VerificationService
import xyz.notebux.util.UserUtil

fun Application.configureRouting() {

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is AuthorizationException -> call.respondText(
                    text = "403: Unauthorized",
                    status = HttpStatusCode.Forbidden
                )
                is NotFoundException -> call.respondText(
                    text = "404: ${cause.message}",
                    status = HttpStatusCode.NotFound
                )
                is ValidationException -> call.respondText(
                    text = "403: ${cause.message}",
                    status = HttpStatusCode.Forbidden
                )
                else -> call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)
            }
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText(text = "This route does not exist! (404)", status = status)
        }

        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respondText(text = "This route does not exist or support this method! (405)", status = status)
        }
    }

    routing {

        get("/") {
            call.respondText("Hello World!")
        }

        post("auth/login") {
            AuthService.handleLogin(this)
        }

        post("auth/register") {
            AuthService.handleRegister(this)
        }

        post("verify_email") {
            VerificationService.handleKeyVerification(this)
        }

        authenticate {
            route("notes") {
                post("new") {
                    NoteService.handleNewNote(this)
                }

                put("edit/{id}") {
                    NoteService.handleEditNote(this)
                }

                get("delete/{id}") {
                    NoteService.handleDeleteNote(this)
                }

                get("user") {
                    NoteService.handleUserNotes(this)
                }

                get("{id}") {
                    NoteService.handleGetNote(this)
                }
            }

            get("@me") {
                val authorizedUser = call.authentication.principal<JwtUser>() ?: throw AuthorizationException()

                val user = UserUtil.mapToUserEntity(transaction {
                    Users.select { Users.id eq authorizedUser.id }.first()
                })

                call.respond(GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(user))
            }
        }
    }
}

class AuthorizationException : RuntimeException()

class ValidationException(override val message: String) : RuntimeException()
