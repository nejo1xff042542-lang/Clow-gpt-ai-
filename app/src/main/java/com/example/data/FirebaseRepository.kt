package com.example.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class UserProfile(
    val uid: String = "",
    val displayName: String = "Guest User",
    val photoUrl: String = ""
)

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUserProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java)
            } else {
                val profile = UserProfile(uid = user.uid)
                firestore.collection("users").document(user.uid).set(profile).await()
                profile
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUserProfile(displayName: String, imageUri: Uri?): UserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            var photoUrl = ""
            if (imageUri != null) {
                val ref = storage.reference.child("profile_images/${user.uid}/${UUID.randomUUID()}")
                ref.putFile(imageUri).await()
                photoUrl = ref.downloadUrl.await().toString()
            } else {
                // Keep old url if no new image
                val oldProfile = getUserProfile()
                photoUrl = oldProfile?.photoUrl ?: ""
            }
            
            val updatedProfile = UserProfile(uid = user.uid, displayName = displayName, photoUrl = photoUrl)
            firestore.collection("users").document(user.uid).set(updatedProfile).await()
            updatedProfile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getSessions(): List<ChatSession> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(user.uid).collection("sessions")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get().await()
            snapshot.toObjects(ChatSession::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createSession(title: String): ChatSession {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        val session = ChatSession(title = title)
        firestore.collection("users").document(user.uid).collection("sessions")
            .document(session.id)
            .set(session).await()
        return session
    }

    suspend fun getMessages(sessionId: String): List<Message> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(user.uid)
                .collection("sessions").document(sessionId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get().await()
            snapshot.toObjects(Message::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveMessage(sessionId: String, message: Message) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .collection("sessions").document(sessionId)
            .collection("messages").document(message.id)
            .set(message).await()
            
        // Update session timestamp
        firestore.collection("users").document(user.uid)
            .collection("sessions").document(sessionId)
            .update("updatedAt", System.currentTimeMillis()).await()
    }
}
