package xyz.notebux.services

import at.favre.lib.crypto.bcrypt.BCrypt
import net.sargue.mailgun.Configuration
import net.sargue.mailgun.Mail
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import xyz.notebux.entities.UserEntity
import xyz.notebux.entities.VerificationCodes
import xyz.notebux.plugins.JwtUser
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object EmailService {

    private const val VERIFICATION_URL = "https://notebux.xyz/verify_email?key="

    private val configuration: Configuration = Configuration()
        .apiUrl("https://api.eu.mailgun.net/v3")
        .domain("mg.notebux.xyz")
        .apiKey("49e5ef9c529e280bc77791484d2d4257-c3d1d1eb-81b8390a")
        .from("Notebux", "noreply@notebux.xyz")

    fun sendVerificationEmail(user: UserEntity) {

        val verificationKey = UUID.randomUUID()

        transaction {
            val verification = VerificationCodes.select { VerificationCodes.user eq user.id }.firstOrNull()

            if (verification == null) {
                VerificationCodes.insert {
                    it[VerificationCodes.user] = user.id
                    it[key] = verificationKey
                    it[expires] = Instant.now().plus(2, ChronoUnit.HOURS)
                }
            } else {
                VerificationCodes.update({ VerificationCodes.user eq user.id }) {
                    it[key] = verificationKey
                    it[expires] = Instant.now().plus(2, ChronoUnit.HOURS)
                }
            }
        }

        Mail.using(configuration)
            .subject("Email verification required")
            .text("Please, confirm your email!")
            .template("email-verification")
            .variables("{\"verificationUrl\": \"$VERIFICATION_URL$verificationKey\"}")
            .to(user.email)
            .build().send()
    }

    fun sendVerifiedEmail(user: UserEntity) {
        Mail.using(configuration)
            .subject("Email verified successfully")
            .text("Your email has been successfully confirmed!")
            .template("email-verified")
            .to(user.email)
            .build().send()
    }

}