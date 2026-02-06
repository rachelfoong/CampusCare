package com.university.campuscare.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface IssuesRepository {
    suspend fun submitIssue(issue: Issue): Flow<DataResult<String>>
    fun getMyIssues(userId: String): Flow<DataResult<List<Issue>>>
    fun getAllIssues(): Flow<DataResult<List<Issue>>>
    suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Flow<DataResult<Boolean>>
    suspend fun getIssueById(issueId: String): Flow<DataResult<Issue>>
}

class IssuesRepositoryImpl(
    private val firestore: FirebaseFirestore
) : IssuesRepository {

    private val issuesCollection = firestore.collection("reports")

    // Create issue in firebase
    override suspend fun submitIssue(issue: Issue): Flow<DataResult<String>> = flow {
        emit(DataResult.Loading)
        try {
            val docRef = if (issue.id.isEmpty()) {
                issuesCollection.document()
            } else {
                issuesCollection.document(issue.id)
            }

            val issueToSave = issue.copy(id = docRef.id, updatedAt = System.currentTimeMillis())
            docRef.set(issueToSave).await()

            emit(DataResult.Success(docRef.id))
        } catch (e: Exception) {
            Log.e("IssuesRepository", "Error in submitIssue: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to submit issue")))
        }
    }

    // Get a user's issues from firebase
    override fun getMyIssues(userId: String): Flow<DataResult<List<Issue>>> = callbackFlow {
        trySend(DataResult.Loading)
        Log.d("IssuesRepository", "Starting query for userId: $userId from reports collection")
        
        val query = issuesCollection
            .whereEqualTo("reportedBy", userId)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("IssuesRepository", "Firestore Error in getMyIssues: ${error.message}")
                trySend(DataResult.Error(Event(error.message ?: "Failed to fetch issues")))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                Log.d("IssuesRepository", "Found ${snapshot.size()} documents in reports")
                val issues = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Issue::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("IssuesRepository", "Mapping failed for doc ${doc.id}: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.createdAt }

                trySend(DataResult.Success(issues))
            } else {
                trySend(DataResult.Success(emptyList()))
            }
        }

        awaitClose { subscription.remove() }
    }

    // Get all issues from firebase for admin
    override fun getAllIssues(): Flow<DataResult<List<Issue>>> = callbackFlow {
        trySend(DataResult.Loading)
        
        val subscription = issuesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("IssuesRepository", "Firestore Error in getAllIssues: ${error.message}")
                    trySend(DataResult.Error(Event(error.message ?: "Error loading issues")))
                    return@addSnapshotListener
                }
                
                val issues = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Issue::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("IssuesRepository", "Mapping failed in getAllIssues for doc ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                trySend(DataResult.Success(issues))
            }

        awaitClose { subscription.remove() }
    }

    // Change issue status in firebase
    override suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Flow<DataResult<Boolean>> = flow {
        emit(DataResult.Loading)
        try {
            issuesCollection.document(issueId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            emit(DataResult.Success(true))
        } catch (e: Exception) {
            Log.e("IssuesRepository", "Error in updateIssueStatus: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to update status")))
        }
    }

    // Get an issue by ID from firebase
    override suspend fun getIssueById(issueId: String): Flow<DataResult<Issue>> = flow {
        emit(DataResult.Loading)
        try {
            val doc = issuesCollection.document(issueId).get().await()
            val issue = doc.toObject(Issue::class.java)?.copy(id = doc.id)

            if (issue != null) {
                emit(DataResult.Success(issue))
            } else {
                emit(DataResult.Error(Event("Issue not found")))
            }
        } catch (e: Exception) {
            Log.e("IssuesRepository", "Error in getIssueById: ${e.message}")
            emit(DataResult.Error(Event(e.message ?: "Failed to fetch issue details")))
        }
    }
}
