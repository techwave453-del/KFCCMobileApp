package com.example.helloworld.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncMessagesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppLocalDatabase.getDatabase(applicationContext)
        val dao = database.chatDao()

        return try {
            val pendingInserts = dao.getPendingInserts()
            for (pending in pendingInserts) {
                val repository = ChatRepository()
                val netResult = repository.sendMessage(pending.roomId, pending.message)
                if (netResult.isSuccess) {
                    // Delete the temporary pending record and let Room insert the official one loaded from server
                    dao.deleteMessageById(pending.id)
                } else {
                    dao.updateSyncStatus(pending.id, SyncStatus.FAILED)
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
