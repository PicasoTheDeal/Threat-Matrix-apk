package com.picasothedeal.threatmatrix.data

import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("api/logs")
    suspend fun getLogs(@Header("Authorization") authToken: String? = null): Response<List<ThreatLog>>

    @POST("api/signup")
    suspend fun signup(@Body request: AuthRequest): Response<SignupResponse>

    @POST("api/login")
    suspend fun login(@Body request: AuthRequest): Response<LoginResponse>

    @GET("api/parameters")
    suspend fun getParameters(@Header("Authorization") authToken: String): Response<ParametersResponse>

    @POST("api/parameters")
    suspend fun updateParameters(
        @Header("Authorization") authToken: String,
        @Body tags: TagsUpdateRequest
    ): Response<Unit>

    @GET("api/interact")
    suspend fun getInteractions(
        @Query("log_ids") logIds: String,
        @Header("Authorization") authToken: String? = null
    ): Response<Map<String, InteractionData>>

    @POST("api/interact")
    suspend fun postInteraction(
        @Header("Authorization") authToken: String? = null,
        @Body request: InteractionRequest
    ): Response<InteractionResponse>
}