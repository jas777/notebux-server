package xyz.notebux.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object VerificationCodes : Table("verification_codes") {

    val user = reference("user", Users.id, ReferenceOption.CASCADE)
    val key = uuid("key")
    val expires = timestamp("expires")

    override val primaryKey = PrimaryKey(user)

}