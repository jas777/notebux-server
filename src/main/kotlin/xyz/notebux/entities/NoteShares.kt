package xyz.notebux.entities

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object NoteShares : Table("note_shares") {
    val userId = reference("user_id", Users.id, onDelete = ReferenceOption.CASCADE)
    val noteId = reference("note_id", Notes.id, onDelete = ReferenceOption.CASCADE)
}