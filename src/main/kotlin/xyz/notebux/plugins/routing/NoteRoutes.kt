package xyz.notebux.plugins.routing

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import xyz.notebux.services.NoteService

// TODO Favorite mails

fun Route.noteRoutes() {
    authenticate {
        post("new") {
            NoteService.handleNewNote(this)
        }

        put("edit/{id}") {
            NoteService.handleEditNote(this)
        }

        get("delete/{id}") {
            NoteService.handleDeleteNote(this)
        }

        get("user") {
            NoteService.handleUserNotes(this)
        }

        get("{id}") {
            NoteService.handleGetNote(this, true)
        }
    }
    get("shared/{id}") {
        NoteService.handleGetNote(this, false)
    }
}