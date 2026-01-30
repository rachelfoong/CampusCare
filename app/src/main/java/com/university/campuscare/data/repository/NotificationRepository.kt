package com.university.campuscare.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Notification
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface NotificationRepository {
    fun createNotification(userId: String, notification: Notification): Flow<DataResult<Unit>>
    fun getNotificationsByUser(userId: String): Flow<DataResult<List<Notification>>>
    fun markAsRead(userId: String, notificationId: String): Flow<DataResult<Unit>>
    fun markAllAsReadForUser(userId: String): Flow<DataResult<Unit>>
    fun deleteNotification(userId: String, notificationId: String): Flow<DataResult<Unit>>
    fun deleteAllNotificationsForUser(userId: String): Flow<DataResult<Unit>>
}

class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private fun getNotificationsCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("notifications")

    // Create notification item in firebase
    override fun createNotification(userId: String, notification: Notification): Flow<DataResult<Unit>> = flow {
        emit(DataResult.Loading)
        try {
            val collection = getNotificationsCollection(userId)
            val docRef = if (notification.id.isEmpty()) {
                collection.document()
            } else {
                collection.document(notification.id)
            }
            
            val notificationToSave = notification.copy(id = docRef.id)
            docRef.set(notificationToSave).await()
            emit(DataResult.Success(Unit))
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error creating notification: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to create notification")))
        }
    }

    // Get all notifications for a user
    override fun getNotificationsByUser(userId: String): Flow<DataResult<List<Notification>>> = flow {
        emit(DataResult.Loading)
        try {
            val snapshot = getNotificationsCollection(userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val notifications = snapshot.toObjects(Notification::class.java)
            emit(DataResult.Success(notifications))
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error fetching notifications: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to fetch notifications")))
        }
    }

    // Mark one notification as read
    override fun markAsRead(userId: String, notificationId: String): Flow<DataResult<Unit>> = flow {
        emit(DataResult.Loading)
        try {
            getNotificationsCollection(userId)
                .document(notificationId)
                .update("isRead", true)
                .await()
            emit(DataResult.Success(Unit))
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking notification as read: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to mark notification as read")))
        }
    }

    // Mark all notifications for a user as read
    override fun markAllAsReadForUser(userId: String): Flow<DataResult<Unit>> = flow {
        emit(DataResult.Loading)
        try {
            val unreadNotifications = getNotificationsCollection(userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            if (!unreadNotifications.isEmpty) {
                val batch = firestore.batch()
                for (document in unreadNotifications.documents) {
                    batch.update(document.reference, "isRead", true)
                }
                batch.commit().await()
            }
            
            emit(DataResult.Success(Unit))
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking all notifications as read: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to mark all notifications as read")))
        }
    }

    // Delete a notification
    override fun deleteNotification(userId: String, notificationId: String): Flow<DataResult<Unit>> = flow {
        emit(DataResult.Loading)
        try {
            getNotificationsCollection(userId)
                .document(notificationId)
                .delete()
                .await()
            emit(DataResult.Success(Unit))
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error deleting notification: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to delete notification")))
        }
    }

    // Delete all notifications for a user
    override fun deleteAllNotificationsForUser(userId: String): Flow<DataResult<Unit>> = flow {
        emit(DataResult.Loading)
        try {
            val notifications = getNotificationsCollection(userId)
                .get()
                .await()

            if(!notifications.isEmpty) {
                val batch = firestore.batch()
                for (document in notifications.documents) {
                    batch.delete(document.reference)
                }
                batch.commit().await()
            }

            emit(DataResult.Success(Unit))
        } catch(e: Exception) {
            Log.e("NotificationRepository", "Error deleting all notifications: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to delete all notifications")))
        }
    }

}
