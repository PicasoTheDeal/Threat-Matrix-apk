package com.picasothedeal.threatmatrix.data

import com.google.gson.annotations.SerializedName

data class ThreatLog(
    val id: String,
    val title: String,
    val excerpt: String,
    val category: String,
    val date: String,
    val readTime: Int,
    val source: String,
    val url: String,
    val impact: String? = null
)

data class Comment(
    val id: Long,
    val content: String,
    @field:SerializedName("created_at") val createdAt: String,
    @field:SerializedName("user_id") val userId: Long,
    val username: String
)

data class InteractionData(
    val likes: Int,
    val liked: Boolean,
    val comments: List<Comment>
)

data class AuthRequest(
    val username: String,
    val password: String,
    val turnstileToken: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String
)

data class SignupResponse(
    val success: Boolean,
    val message: String,
    val token: String
)

data class ParametersResponse(
    val tags: List<String>
)

data class TagsUpdateRequest(
    val tags: List<String>
)

data class InteractionRequest(
    val action: String,
    @field:SerializedName("log_id") val logId: String,
    val content: String? = null,
    @field:SerializedName("comment_id") val commentId: Long? = null,
    val turnstileToken: String? = null
)

data class InteractionResponse(
    val success: Boolean,
    val interaction: InteractionData?
)

data class ApiError(
    val error: String
)