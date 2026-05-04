package com.example.contentanalyzer.domain.utils

import kotlinx.coroutines.delay
import kotlin.math.pow

object RetryHandler {

    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val isLastAttempt = attempt == maxRetries - 1

                if (!isLastAttempt && shouldRetry(e)) {
                    val delayMs = (currentDelay * (2.0.pow(attempt))).toLong()
                        .coerceAtMost(maxDelayMs)

                    println("Retry attempt ${attempt + 1}/$maxRetries after ${delayMs}ms")
                    delay(delayMs)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: Exception("Retry failed")
    }

    private fun shouldRetry(e: Exception): Boolean {
        return when (e) {
            is retrofit2.HttpException -> {
                when (e.code()) {
                    408, 429, 500, 502, 503, 504 -> true  // Retry on server errors
                    410 -> false  // Don't retry on 410 - endpoint gone
                    else -> false
                }
            }
            is java.io.IOException -> true  // Retry on network errors
            else -> false
        }
    }
}
