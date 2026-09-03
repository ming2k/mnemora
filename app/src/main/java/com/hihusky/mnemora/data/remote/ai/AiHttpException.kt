package com.hihusky.mnemora.data.remote.ai

/** Raised by a provider adapter when the streaming endpoint returns a non-success status. */
class AiHttpException(
    message: String,
) : Exception(message)
