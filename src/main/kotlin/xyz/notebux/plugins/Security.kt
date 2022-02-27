package xyz.notebux.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.notebux.entities.UserEntity
import xyz.notebux.entities.Users
import java.time.Instant
import java.util.*

fun Application.configureSecurity() {

    authentication {
        jwt {
            val jwtAudience = environment.config.property("jwt.audience").getString()
            realm = environment.config.property("jwt.realm").getString()
            verifier(
                JWT
                    .require(Algorithm.HMAC256(environment.config.property("jwt.secret").getString()))
                    .withAudience(jwtAudience)
                    .withIssuer(environment.config.property("jwt.domain").getString())
                    .build()
            )
            validate { credential ->
                if (credential.payload.claims.containsKey("email") && credential.payload.claims.containsKey("id")) {
                    JwtUser(
                        credential.payload.getClaim("email").asString(),
                        UUID.fromString(credential.payload.getClaim("id").asString())
                    )
                } else null
            }
        }
    }

}

data class JwtUser(val email: String, val id: UUID) : Principal

data class AuthBody(val email: String, val password: String)
