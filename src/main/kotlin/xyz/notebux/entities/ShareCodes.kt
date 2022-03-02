package xyz.notebux.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ShareCodes : Table("share_codes") {
    val noteId = reference("note_id", Notes.id, ReferenceOption.CASCADE)
    val code = varchar("code", 64);

    override val primaryKey = PrimaryKey(noteId)
}
