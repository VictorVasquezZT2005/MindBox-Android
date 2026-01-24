package xyz.zt.mindbox.ui.dashboard.screens.reminders

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import xyz.zt.mindbox.data.model.Reminder

class RemindersViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _reminders = mutableStateListOf<Reminder>()
    val reminders: List<Reminder> get() = _reminders

    init {
        cargarRecordatoriosDesdeFirebase()
    }

    fun addReminder(reminder: Reminder) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("reminders")
            .document(reminder.id).set(reminder)
    }

    fun updateReminder(reminder: Reminder) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("reminders")
            .document(reminder.id).set(reminder)
    }

    fun deleteReminder(reminderId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("reminders")
            .document(reminderId).delete()
    }

    private fun cargarRecordatoriosDesdeFirebase() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("reminders")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _reminders.clear()
                    val items = snapshot.toObjects(Reminder::class.java)
                    _reminders.addAll(items)
                }
            }
    }
}