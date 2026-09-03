package com.hihusky.mnemora.data.model

sealed class ImportResult {
    data class Success(
        val packageName: String?,
    ) : ImportResult()

    data class Error(
        val errorMessage: String,
    ) : ImportResult()

    data object Cancelled : ImportResult()

    val isSuccess: Boolean get() = this is Success
    val isCancelled: Boolean get() = this is Cancelled
}
