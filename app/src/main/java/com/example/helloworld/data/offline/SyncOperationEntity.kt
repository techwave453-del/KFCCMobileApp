package com.example.helloworld.data.offline

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_operations",
    indices = [Index(value = ["status"]), Index(value = ["createdAt"])]
)
data class SyncOperationEntity(
    @androidx.room.PrimaryKey val operationId: String,
    val entityType: String,
    val operationType: String,
    val entityId: String?,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val status: String = "pending",
    val lastError: String? = null
)
