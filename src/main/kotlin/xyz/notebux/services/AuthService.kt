package xyz.notebux.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.notebux.entities.Users
import xyz.notebux.plugins.AuthBody
import xyz.notebux.plugins.JwtUser
import xyz.notebux.plugins.ValidationException
import xyz.notebux.util.UserUtil
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object AuthService {

    suspend fun handleRegister(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val user = call.receive<AuthBody>()

        val result = transaction {
            Users.select {
                Users.email eq user.email
            }.firstOrNull()
        }

        if (result != null) {
            throw ValidationException("A user with that email already exists!")
        } else {
            val insertedUser = transaction {
                Users.insert {
                    it[email] = user.email
                    it[password] = BCrypt.withDefaults().hashToString(12, user.password.toCharArray())
                }.resultedValues?.first()
            } ?: error("Unexpected database error!")

            call.respondText {
                GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(UserUtil.mapToUserEntity(insertedUser))
            }
        }
    }

    suspend fun handleLogin(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val user = call.receive<AuthBody>()

        val result = transaction {
            Users.select {
                Users.email eq user.email
            }.firstOrNull()
        }

        if (result == null) {
            throw ValidationException("Invalid email or password!")
        } else {
            if (BCrypt.verifyer().verify(user.password.toCharArray(), result[Users.password]).verified) {
                val generatedJwt = JWT.create()
                    .withIssuer(application.environment.config.property("jwt.domain").getString())
                    .withAudience(application.environment.config.property("jwt.audience").getString())
                    .withClaim("email", result[Users.email])
                    .withClaim("id", result[Users.id].toString())
                    .withExpiresAt(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
                    .sign(Algorithm.HMAC256(application.environment.config.property("jwt.secret").getString()))

                call.respond(generatedJwt)
            } else {
                throw ValidationException("Invalid email or password!")
            }
        }
    }

}