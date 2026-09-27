package com.notes.server.repository

import com.notes.common.models.Note
import com.notes.common.models.SyncRequest
import com.notes.common.models.SyncResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

interface SyncRepository {
    fun sync(userId: String, request: SyncRequest): SyncResponse
    fun getNote(userId: String, noteId: String): Note?
    fun saveNote(userId: String, note: Note): Note
    fun softDeleteNote(userId: String, noteId: String): Boolean
    fun getTrashNotes(userId: String): List<Note>
    fun restoreTrashNote(userId: String, noteId: String): Note?
    fun purgeTrashNote(userId: String, noteId: String): Boolean
    fun emptyTrash(userId: String): Int
    fun getRevisions(userId: String, noteId: String): List<Note>
    fun clear()
}

class InMemorySyncRepository : SyncRepository {
    // userId -> (noteId -> Note)
    private val userNotes = ConcurrentHashMap<String, ConcurrentHashMap<String, Note>>()
    // userId -> (noteId -> List<Note>)
    private val userRevisions = ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<Note>>>()

    private fun getOrCreateNotes(userId: String): ConcurrentHashMap<String, Note> {
        return userNotes.computeIfAbsent(userId) { ConcurrentHashMap() }
    }

    private fun getOrCreateRevisions(userId: String): ConcurrentHashMap<String, CopyOnWriteArrayList<Note>> {
        return userRevisions.computeIfAbsent(userId) { ConcurrentHashMap() }
    }

    override fun sync(userId: String, request: SyncRequest): SyncResponse {
        val notes = getOrCreateNotes(userId)
        val revisions = getOrCreateRevisions(userId)
        val now = System.currentTimeMillis()
        var conflictsResolved = 0

        // 1. Process soft deletions requested by client
        for (deletedId in request.deletedNoteIds) {
            val existing = notes[deletedId]
            if (existing != null && !existing.isDeleted) {
                notes[deletedId] = existing.copy(
                    isDeleted = true,
                    deletedAt = now,
                    updatedAt = now
                )
            }
        }

        // 2. Process modified notes from client (LWW resolution)
        for (clientNote in request.modifiedNotes) {
            val existing = notes[clientNote.id]
            if (existing == null) {
                // New note from client
                notes[clientNote.id] = clientNote.copy(
                    version = maxOf(1L, clientNote.version),
                    isDeleted = false,
                    deletedAt = null
                )
            } else {
                if (clientNote.updatedAt >= existing.updatedAt) {
                    // Client wins (LWW)
                    revisions.computeIfAbsent(clientNote.id) { CopyOnWriteArrayList() }.add(existing)
                    notes[clientNote.id] = clientNote.copy(
                        version = existing.version + 1L,
                        isDeleted = false,
                        deletedAt = null
                    )
                } else {
                    // Server wins (LWW Conflict)
                    conflictsResolved++
                }
            }
        }

        // 3. Compute server updates created/modified strictly after lastSyncedTimestamp
        val serverUpdates = notes.values.filter { note ->
            !note.isDeleted && note.updatedAt > request.lastSyncedTimestamp
        }.sortedBy { it.updatedAt }

        // 4. Compute server deletions after lastSyncedTimestamp
        val serverDeletions = notes.values.filter { note ->
            note.isDeleted && (note.deletedAt ?: note.updatedAt) > request.lastSyncedTimestamp
        }.map { it.id }

        return SyncResponse(
            serverTimestamp = now,
            serverUpdates = serverUpdates,
            serverDeletions = serverDeletions,
            conflictsResolved = conflictsResolved
        )
    }

    override fun getNote(userId: String, noteId: String): Note? {
        val note = getOrCreateNotes(userId)[noteId]
        return if (note != null && !note.isDeleted) note else null
    }

    override fun saveNote(userId: String, note: Note): Note {
        val notes = getOrCreateNotes(userId)
        val revisions = getOrCreateRevisions(userId)
        val existing = notes[note.id]
        val updatedNote = if (existing != null) {
            revisions.computeIfAbsent(note.id) { CopyOnWriteArrayList() }.add(existing)
            note.copy(version = existing.version + 1L, updatedAt = System.currentTimeMillis())
        } else {
            note.copy(updatedAt = System.currentTimeMillis())
        }
        notes[note.id] = updatedNote
        return updatedNote
    }

    override fun softDeleteNote(userId: String, noteId: String): Boolean {
        val notes = getOrCreateNotes(userId)
        val existing = notes[noteId] ?: return false
        if (existing.isDeleted) return false
        val now = System.currentTimeMillis()
        notes[noteId] = existing.copy(isDeleted = true, deletedAt = now, updatedAt = now)
        return true
    }

    override fun getTrashNotes(userId: String): List<Note> {
        return getOrCreateNotes(userId).values
            .filter { it.isDeleted }
            .sortedByDescending { it.deletedAt ?: it.updatedAt }
    }

    override fun restoreTrashNote(userId: String, noteId: String): Note? {
        val notes = getOrCreateNotes(userId)
        val existing = notes[noteId] ?: return null
        if (!existing.isDeleted) return existing
        val now = System.currentTimeMillis()
        val restored = existing.copy(
            isDeleted = false,
            deletedAt = null,
            updatedAt = now,
            version = existing.version + 1L
        )
        notes[noteId] = restored
        return restored
    }

    override fun purgeTrashNote(userId: String, noteId: String): Boolean {
        val notes = getOrCreateNotes(userId)
        val removed = notes.remove(noteId) != null
        getOrCreateRevisions(userId).remove(noteId)
        return removed
    }

    override fun emptyTrash(userId: String): Int {
        val notes = getOrCreateNotes(userId)
        val trashIds = notes.values.filter { it.isDeleted }.map { it.id }
        for (id in trashIds) {
            notes.remove(id)
            getOrCreateRevisions(userId).remove(id)
        }
        return trashIds.size
    }

    override fun getRevisions(userId: String, noteId: String): List<Note> {
        return getOrCreateRevisions(userId)[noteId]?.toList() ?: emptyList()
    }

    override fun clear() {
        userNotes.clear()
        userRevisions.clear()
    }
}

val defaultSyncRepository: SyncRepository = InMemorySyncRepository()
