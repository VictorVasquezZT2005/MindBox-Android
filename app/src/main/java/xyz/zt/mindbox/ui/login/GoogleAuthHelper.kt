package xyz.zt.mindbox.ui.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

object GoogleAuthHelper {

    suspend fun doGoogleSignIn(context: Context): FirebaseUser? {
        val credentialManager = CredentialManager.create(context)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val WEB_CLIENT_ID = "1034699988850-phitvndqupeb24tgdssd79ffej816voq.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is GoogleIdTokenCredential) {
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user

                // Sincronizar con Firestore si el usuario es nuevo
                user?.let {
                    val userDoc = db.collection("users").document(it.uid).get().await()
                    if (!userDoc.exists()) {
                        val userData = hashMapOf(
                            "name" to (it.displayName ?: "Usuario Google"),
                            "email" to (it.email ?: ""),
                            "lastUpdate" to Date()
                        )
                        db.collection("users").document(it.uid).set(userData).await()
                    }
                }
                user
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}