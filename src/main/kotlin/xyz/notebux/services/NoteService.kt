package xyz.notebux.services

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.notebux.entities.*
import xyz.notebux.plugins.AuthorizationException
import xyz.notebux.plugins.JwtUser
import xyz.notebux.plugins.ValidationException
import xyz.notebux.util.NoteUtil
import java.lang.IllegalArgumentException
import java.time.Instant
import java.util.*

object NoteService {

    suspend fun handleNewNote(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val noteData = call.receive<NoteDto>()
        val authorizedUser = call.authentication.principal<JwtUser>() ?: throw AuthorizationException()

        val note = transaction {
            val user = Users.select {
                Users.id eq authorizedUser.id
            }.firstOrNull() ?: error("Unexpected database error")

            val inserted = Notes.insert {
                it[title] = noteData.title
                it[content] = noteData.content
                it[author] = user[Users.id]
                it[createdAt] = Instant.now()
                it[lastEdited] = Instant.now()
                it[sharedGlobally] = false
            }

            noteData.shareTo.forEach {
                val noteUser = Users.select { Users.email eq it }.firstOrNull()
                if (noteUser === null) return@forEach
                NoteShares.insert {
                    it[userId] = noteUser[Users.id]
                    it[noteId] = inserted[Notes.id]
                }
            }

            Notes.select { Notes.id eq inserted[Notes.id] }.first()
        }

        call.respondText { Gson().toJson(NoteUtil.mapToNoteEntity(note)) }
    }

    suspend fun handleEditNote(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val noteData = call.receive<NoteDto>()
        val authorizedUser = call.authentication.principal<JwtUser>() ?: throw AuthorizationException()

        val noteUuid = getUUID(pipelineContext) ?: throw ValidationException("Invalid UUID!")

        val editedNote = transaction {
            val note = Notes.select { Notes.id eq noteUuid }.firstOrNull() ?: throw NotFoundException("Invalid note!")

            if (note[Notes.author] != authorizedUser.id) throw AuthorizationException()

            NoteShares.deleteWhere { NoteShares.noteId eq noteUuid }

            noteData.shareTo.forEach { shareEmail ->
                val shareToUser = Users.select { Users.email eq shareEmail }.firstOrNull() ?: return@forEach

                NoteShares.insert {
                    it[userId] = shareToUser[Users.id]
                    it[noteId] = noteUuid
                }
            }

            Notes.update({ Notes.id eq noteUuid }) {
                it[title] = noteData.title
                it[content] = noteData.content
                it[lastEdited] = Instant.now()
                it[sharedGlobally] = noteData.sharedGlobally
            }

            Notes.select { Notes.id eq noteUuid }.first()
        }

        call.respond(Gson().toJson(NoteUtil.mapToNoteEntity(editedNote)))
    }

    suspend fun handleDeleteNote(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val authorizedUser = call.authentication.principal<JwtUser>() ?: throw AuthorizationException()
        val noteUuid = getUUID(pipelineContext) ?: throw ValidationException("Invalid UUID!")

        transaction {
            val note = Notes.select { Notes.id eq noteUuid }.firstOrNull() ?: throw NotFoundException("Note not found!")

            if (note[Notes.author] != authorizedUser.id) throw AuthorizationException()

            Notes.deleteWhere { Notes.id eq note[Notes.id] }
        }

        call.respond(HttpStatusCode.OK, "Note deleted successfully")
    }

    suspend fun handleUserNotes(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val authorizedUser = call.authentication.principal<JwtUser>() ?: throw AuthorizationException()

        val notes = transaction {
            (NoteShares.innerJoin(Notes innerJoin Users, { noteId }, { Notes.id }))
                .slice(
                    Users.email,
                    Notes.id,
                    Notes.title,
                    Notes.author,
                    Notes.content,
                    Notes.createdAt,
                    Notes.lastEdited
                )
                .select { NoteShares.userId eq authorizedUser.id }
                .union(
                    (Notes innerJoin Users).slice(
                        Users.email,
                        Notes.id,
                        Notes.title,
                        Notes.author,
                        Notes.content,
                        Notes.createdAt,
                        Notes.lastEdited
                    )
                        .select { Notes.author eq authorizedUser.id })
                .map {
                    Note(
                        it[Notes.title],
                        "",
                        it[Users.email],
                        it[Notes.id].toString(),
                        it[Notes.createdAt].toEpochMilli(),
                        it[Notes.lastEdited].toEpochMilli(),
                        null,
                        false
                    )
                }
        }

        call.respond(Gson().toJson(notes))

    }

    suspend fun handleGetNote(pipelineContext: PipelineContext<Unit, ApplicationCall>) = with(pipelineContext) {
        val authorizedUser = call.authentication.principal<JwtUser>() ?: throw AuthorizationException()
        val noteUuid = getUUID(pipelineContext) ?: throw ValidationException("Invalid UUID!")

        val note = transaction {
            Notes.select { Notes.id eq noteUuid }.firstOrNull()
        }

        if (note === null) call.respond(HttpStatusCode.NotFound, "Invalid note")
        else {

            val emails = NoteUtil.getNoteSharedEmails(noteUuid)

            if (note[Notes.author] != authorizedUser.id && !emails.contains(
                    authorizedUser.email
                )
            ) throw AuthorizationException()

            call.respondText {
                Gson().toJson(
                    Note(
                        note[Notes.title],
                        note[Notes.content],
                        note[Notes.author].toString(),
                        note[Notes.id].toString(),
                        note[Notes.createdAt].toEpochMilli(),
                        note[Notes.lastEdited].toEpochMilli(),
                        emails,
                        note[Notes.sharedGlobally]
                    )
                )
            }
        }
    }

    private fun getUUID(pipelineContext: PipelineContext<Unit, ApplicationCall>): UUID? = with(pipelineContext) {
        val noteId = call.parameters["id"]
        val noteUuid: UUID;

        try {
            noteUuid = UUID.fromString(noteId)
        } catch (ex: IllegalArgumentException) {
            return null
        }

        return noteUuid
    }

}