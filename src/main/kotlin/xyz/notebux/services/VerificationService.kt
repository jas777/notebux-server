package xyz.notebux.services

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import xyz.notebux.entities.Users
import xyz.notebux.entities.VerificationCodes
import xyz.notebux.plugins.ValidationException
import xyz.notebux.util.UserUtil
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*

object VerificationService {

    private data class KeyDTO(val key: String)

    suspend fun handleKeyVerification(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val key = call.receive<KeyDTO>()

        val keyUUID: UUID = try {
            UUID.fromString(key.key)
        } catch (ex: IllegalArgumentException) {
            null
        } ?: throw ValidationException("Invalid verification key!")

        val verifyingUser = transaction {
            (VerificationCodes innerJoin Users)
                .slice(VerificationCodes.key, VerificationCodes.expires, Users.id, Users.email, Users.activated, Users.password)
                .select { VerificationCodes.key eq keyUUID }
                .firstOrNull()
        } ?: throw ValidationException("Invalid verification key!")

        if (verifyingUser[VerificationCodes.expires].isBefore(Instant.now())) {
            throw ValidationException("Verification key expired!")
        }

        transaction {
            Users.update({ Users.id eq verifyingUser[Users.id] }) {
                it[activated] = true
            }

            VerificationCodes.deleteWhere { VerificationCodes.user eq verifyingUser[Users.id] }
        }

        EmailService.sendVerifiedEmail(UserUtil.mapToUserEntity(verifyingUser))

        call.respondText { "Account verified!" }
    }

}