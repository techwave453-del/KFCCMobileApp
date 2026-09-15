package com.example.helloworld.admin

import android.content.Context

/**
 * Application-scoped owner of the administrator API client.
 *
 * All admin modules must use this provider so authentication state, the
 * encrypted session store, and the Ktor client are shared across the entire
 * admin surface. This prevents one module from creating an isolated client
 * while another module is refreshing or replacing the administrator session.
 */
object AdminRepositoryProvider {
    @Volatile
    private var instance: AdminRepository? = null

    fun get(context: Context): AdminRepository {
        return instance ?: synchronized(this) {
            instance ?: AdminRepository(context.applicationContext).also {
                instance = it
            }
        }
    }
}
