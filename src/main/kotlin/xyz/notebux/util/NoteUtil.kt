package xyz.notebux.util

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.notebux.entities.Note
import xyz.notebux.entities.NoteShares
import xyz.notebux.entities.Notes
import xyz.notebux.entities.Users
import java.util.*

object NoteUtil {

    fun mapToNoteEntity(row: ResultRow): Note {
        return Note(
            row[Notes.title],
            row[Notes.content],
            row[Notes.author].toString(),
            row[Notes.id].toString(),
            row[Notes.createdAt].toEpochMilli(),
            row[Notes.lastEdited].toEpochMilli(),
            null,
            row[Notes.sharedGlobally]
        )
    }

    fun getNoteSharedEmails(noteUUID: UUID): List<String> {
        return transaction {
            val joinedTable = NoteShares.innerJoin(Notes, { Notes.id }, { NoteShares.noteId })
            Users.innerJoin(joinedTable, { NoteShares.userId }, { Users.id })
                .slice(Users.email)
                .select { Notes.id eq noteUUID }
                .map { it[Users.email] }
        }
    }

}