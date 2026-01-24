package xyz.zt.mindbox.ui.dashboard.screens.notes

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import xyz.zt.mindbox.data.model.Note

class NotesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _notes = mutableStateListOf<Note>()

    var searchQuery by mutableStateOf("")
    var selectedTypeFilter by mutableStateOf("Todas")

    val notes: List<Note> get() {
        val searchFiltered = if (searchQuery.isEmpty()) _notes
        else _notes.filter { it.content.contains(searchQuery, ignoreCase = true) }

        return if (selectedTypeFilter == "Todas") searchFiltered
        else searchFiltered.filter { it.type == selectedTypeFilter }
    }

    private var firestoreListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init { setupAuthObserver() }

    private fun setupAuthObserver() {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) listenToNotes(user.uid) else _notes.clear()
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun listenToNotes(userId: String) {
        firestoreListener?.remove()
        firestoreListener = db.collection("users").document(userId).collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val items = it.toObjects(Note::class.java)
                    _notes.clear()
                    _notes.addAll(items)
                }
            }
    }

    fun addNote(content: String, type: String, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false)
        val docRef = db.collection("users").document(user.uid).collection("notes").document()
        val newNote = Note(docRef.id, user.uid, content, type, System.currentTimeMillis())
        docRef.set(newNote).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateNote(noteId: String, content: String, type: String, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false)
        db.collection("users").document(user.uid).collection("notes").document(noteId)
            .update("content", content, "type", type, "timestamp", System.currentTimeMillis())
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteNote(noteId: String) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).collection("notes").document(noteId).delete()
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) }
        firestoreListener?.remove()
    }
}