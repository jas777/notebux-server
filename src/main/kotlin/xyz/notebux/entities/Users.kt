package xyz.notebux.entities

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.jetbrains.exposed.sql.Table
import java.util.*

object Users : Table("users") {
    val id = uuid("id").uniqueIndex().autoGenerate()
    val email = varchar("email", 256)
    val password = varchar("password", 400)
    val activated = bool("activated")
    override val primaryKey = PrimaryKey(id)
}

data class UserEntity(@Expose val id: UUID, @Expose val email: String, val password: String, @Expose val activated: Boolean)