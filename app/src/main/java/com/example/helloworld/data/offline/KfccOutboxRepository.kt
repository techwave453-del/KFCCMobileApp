package com.example.helloworld.data.offline

import android.content.Context
import java.util.UUID

class KfccOutboxRepository(
    private val context: Context,
    private val db: KfccDatabase = KfccDatabase.getInstance(context)
) {
    suspend fun enqueue(
        entityType: String,
        operationType: String,
        entityId: String?,
        payload: String
    ) {
        db.syncOperationDao().insert(
            SyncOperationEntity(
                operationId = UUID.randomUUID().toString(),
                entityType = entityType,
                operationType = operationType,
                entityId = entityId,
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        )
        KfccContentSyncScheduler.syncNow(context)
    }
}
