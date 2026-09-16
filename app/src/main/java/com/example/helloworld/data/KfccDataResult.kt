package com.example.helloworld.data

/**
 * Small result wrapper shared by database-backed repositories.
 */
sealed interface KfccDataResult<out T> {
    data class Success<T>(val value: T) : KfccDataResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : KfccDataResult<Nothing>
}
