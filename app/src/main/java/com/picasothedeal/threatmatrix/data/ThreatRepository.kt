package com.picasothedeal.threatmatrix.data

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

@Suppress("unused")
class ThreatRepository(
    private val api: ApiService,
    private val gson: Gson = Gson(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private fun formatBearer(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    private fun requireBearer(token: String): String {
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    suspend fun fetchLogs(authToken: String? = null): Result<List<ThreatLog>> =
        safeApiCall { api.getLogs(formatBearer(authToken)) }

    suspend fun signup(username: String, password: String, turnstileToken: String): Result<SignupResponse> =
        safeApiCall { api.signup(AuthRequest(username, password, turnstileToken)) }

    suspend fun login(username: String, password: String, turnstileToken: String): Result<LoginResponse> =
        safeApiCall { api.login(AuthRequest(username, password, turnstileToken)) }

    suspend fun fetchParameters(authToken: String): Result<ParametersResponse> =
        safeApiCall { api.getParameters(requireBearer(authToken)) }

    suspend fun updateParameters(authToken: String, tags: List<String>): Result<Unit> =
        safeApiCall { api.updateParameters(requireBearer(authToken), TagsUpdateRequest(tags)) }

    suspend fun fetchInteractions(logIds: List<String>, authToken: String? = null): Result<Map<String, InteractionData>> =
        safeApiCall { api.getInteractions(logIds.joinToString(","), formatBearer(authToken)) }

    suspend fun like(logId: String, authToken: String): Result<InteractionResponse> =
        safeApiCall { api.postInteraction(requireBearer(authToken), InteractionRequest("like", logId)) }

    suspend fun unlike(logId: String, authToken: String): Result<InteractionResponse> =
        safeApiCall { api.postInteraction(requireBearer(authToken), InteractionRequest("unlike", logId)) }

    suspend fun comment(logId: String, content: String, turnstileToken: String, authToken: String): Result<InteractionResponse> =
        safeApiCall {
            api.postInteraction(
                requireBearer(authToken),
                InteractionRequest(
                    action = "comment",
                    logId = logId,
                    content = content,
                    turnstileToken = turnstileToken
                )
            )
        }

    suspend fun editComment(logId: String, commentId: Long, newContent: String, authToken: String): Result<InteractionResponse> =
        safeApiCall {
            api.postInteraction(
                requireBearer(authToken),
                InteractionRequest(
                    action = "edit_comment",
                    logId = logId,
                    commentId = commentId,
                    content = newContent
                )
            )
        }

    suspend fun deleteComment(logId: String, commentId: Long, authToken: String): Result<InteractionResponse> =
        safeApiCall {
            api.postInteraction(
                requireBearer(authToken),
                InteractionRequest(
                    action = "delete_comment",
                    logId = logId,
                    commentId = commentId
                )
            )
        }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> =
        withContext(ioDispatcher) {
            try {
                val response = call()
                if (response.isSuccessful) {
                    response.body()?.let { Result.success(it) }
                        ?: Result.failure(Exception("Empty response body"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = errorBody?.let { body ->
                        try {
                            gson.fromJson(body, ApiError::class.java).error
                        } catch (_: Exception) {
                            body
                        }
                    } ?: "HTTP ${response.code()}"
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}