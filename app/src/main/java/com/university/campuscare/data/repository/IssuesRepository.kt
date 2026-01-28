package com.university.campuscare.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.utils.DataResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface IssuesRepository {
    suspend fun submitIssue(issue: Issue): Flow<DataResult<String>>
    fun getMyIssues(userId: String): Flow<DataResult<List<Issue>>>
    fun getAllIssues(): Flow<DataResult<List<Issue>>>
    suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Flow<DataResult<Boolean>>
    suspend fun assignIssue(issueId: String, assignedTo: String): Flow<DataResult<Boolean>>
    suspend fun getIssueById(issueId: String): Flow<DataResult<Issue>>
}

class IssuesRepositoryImpl(
    private val firestore: FirebaseFirestore
) : IssuesRepository {
    
    private val issuesCollection = firestore.collection("issues")
    
    override suspend fun submitIssue(issue: Issue): Flow<DataResult<String>> = callbackFlow {
        trySend(DataResult.Loading)
        
        try {
            val issueData = hashMapOf(
                "category" to issue.category,
                "title" to issue.title,
                "description" to issue.description,
                "location" to hashMapOf(
                    "block" to issue.location.block,
                    "level" to issue.location.level,
                    "room" to issue.location.room,
                    "latitude" to issue.location.latitude,
                    "longitude" to issue.location.longitude
                ),
                "status" to issue.status.name,
                "reportedBy" to issue.reportedBy,
                "reporterName" to issue.reporterName,
                "assignedTo" to issue.assignedTo,
                "photoUrl" to issue.photoUrl,
                "createdAt" to issue.createdAt,
                "updatedAt" to issue.updatedAt
            )
            
            val docRef = issuesCollection.add(issueData).await()
            trySend(DataResult.Success(docRef.id))
        } catch (e: Exception) {
            trySend(DataResult.Error(com.university.campuscare.utils.Event(e.message ?: "Failed to submit issue")))
        }
        
        awaitClose()
    }
    
    override fun getMyIssues(userId: String): Flow<DataResult<List<Issue>>> = callbackFlow {
        trySend(DataResult.Loading)
        
        val subscription = issuesCollection
            .whereEqualTo("reportedBy", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataResult.Error(com.university.campuscare.utils.Event(error.message ?: "Failed to load issues")))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val issues = snapshot.documents.mapNotNull { doc ->
                        try {
                            val locationData = doc.get("location") as? Map<*, *>
                            Issue(
                                id = doc.id,
                                category = doc.getString("category") ?: "",
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                location = com.university.campuscare.data.model.IssueLocation(
                                    block = locationData?.get("block") as? String ?: "",
                                    level = locationData?.get("level") as? String ?: "",
                                    room = locationData?.get("room") as? String ?: "",
                                    latitude = locationData?.get("latitude") as? Double,
                                    longitude = locationData?.get("longitude") as? Double
                                ),
                                status = IssueStatus.valueOf(doc.getString("status") ?: "PENDING"),
                                reportedBy = doc.getString("reportedBy") ?: "",
                                reporterName = doc.getString("reporterName") ?: "",
                                assignedTo = doc.getString("assignedTo"),
                                photoUrl = doc.getString("photoUrl"),
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(DataResult.Success(issues))
                }
            }
        
        awaitClose { subscription.remove() }
    }
    
    override fun getAllIssues(): Flow<DataResult<List<Issue>>> = callbackFlow {
        trySend(DataResult.Loading)
        
        val subscription = issuesCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataResult.Error(com.university.campuscare.utils.Event(error.message ?: "Failed to load issues")))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val issues = snapshot.documents.mapNotNull { doc ->
                        try {
                            val locationData = doc.get("location") as? Map<*, *>
                            Issue(
                                id = doc.id,
                                category = doc.getString("category") ?: "",
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                location = com.university.campuscare.data.model.IssueLocation(
                                    block = locationData?.get("block") as? String ?: "",
                                    level = locationData?.get("level") as? String ?: "",
                                    room = locationData?.get("room") as? String ?: "",
                                    latitude = locationData?.get("latitude") as? Double,
                                    longitude = locationData?.get("longitude") as? Double
                                ),
                                status = IssueStatus.valueOf(doc.getString("status") ?: "PENDING"),
                                reportedBy = doc.getString("reportedBy") ?: "",
                                reporterName = doc.getString("reporterName") ?: "",
                                assignedTo = doc.getString("assignedTo"),
                                photoUrl = doc.getString("photoUrl"),
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(DataResult.Success(issues))
                }
            }
        
        awaitClose { subscription.remove() }
    }
    
    override suspend fun updateIssueStatus(issueId: String, status: IssueStatus): Flow<DataResult<Boolean>> = callbackFlow {
        trySend(DataResult.Loading)
        
        try {
            issuesCollection.document(issueId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            trySend(DataResult.Success(true))
        } catch (e: Exception) {
            trySend(DataResult.Error(com.university.campuscare.utils.Event(e.message ?: "Failed to update status")))
        }
        
        awaitClose()
    }
    
    override suspend fun assignIssue(issueId: String, assignedTo: String): Flow<DataResult<Boolean>> = callbackFlow {
        trySend(DataResult.Loading)
        
        try {
            issuesCollection.document(issueId)
                .update(
                    mapOf(
                        "assignedTo" to assignedTo,
                        "status" to IssueStatus.IN_PROGRESS.name,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            trySend(DataResult.Success(true))
        } catch (e: Exception) {
            trySend(DataResult.Error(com.university.campuscare.utils.Event(e.message ?: "Failed to assign issue")))
        }
        
        awaitClose()
    }
    
    override suspend fun getIssueById(issueId: String): Flow<DataResult<Issue>> = callbackFlow {
        trySend(DataResult.Loading)
        
        try {
            val doc = issuesCollection.document(issueId).get().await()
            if (doc.exists()) {
                val locationData = doc.get("location") as? Map<*, *>
                val issue = Issue(
                    id = doc.id,
                    category = doc.getString("category") ?: "",
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    location = com.university.campuscare.data.model.IssueLocation(
                        block = locationData?.get("block") as? String ?: "",
                        level = locationData?.get("level") as? String ?: "",
                        room = locationData?.get("room") as? String ?: "",
                        latitude = locationData?.get("latitude") as? Double,
                        longitude = locationData?.get("longitude") as? Double
                    ),
                    status = IssueStatus.valueOf(doc.getString("status") ?: "PENDING"),
                    reportedBy = doc.getString("reportedBy") ?: "",
                    reporterName = doc.getString("reporterName") ?: "",
                    assignedTo = doc.getString("assignedTo"),
                    photoUrl = doc.getString("photoUrl"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                trySend(DataResult.Success(issue))
            } else {
                trySend(DataResult.Error(com.university.campuscare.utils.Event("Issue not found")))
            }
        } catch (e: Exception) {
            trySend(DataResult.Error(com.university.campuscare.utils.Event(e.message ?: "Failed to load issue")))
        }
        
        awaitClose()
    }
}
