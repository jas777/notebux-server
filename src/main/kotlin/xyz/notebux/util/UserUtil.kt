package xyz.notebux.util

import org.jetbrains.exposed.sql.ResultRow
import xyz.notebux.entities.UserEntity
import xyz.notebux.entities.Users

object UserUtil {

    fun mapToUserEntity(row: ResultRow): UserEntity {
        return UserEntity(row[Users.id], row[Users.email], row[Users.password], row[Users.activated])
    }

}