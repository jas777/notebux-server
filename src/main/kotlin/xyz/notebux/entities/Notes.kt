package xyz.notebux.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Notes : Table("notes") {
    val id = uuid("id").uniqueIndex().autoGenerate()
    val title = varchar("title", 64)
    val content = varchar("content", 100000)
    val author = reference("author", Users.id)
    val lastEdited = timestamp("last_edit")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

data class NoteDto(val title: String, val content: String, val shareTo: List<String>)

data class Note(
    val title: String,
    val content: String,
    val author: String,
    val id: String,
    val createdAt: Long,
    val lastEdited: Long,
    val shareTo: List<String>?
)