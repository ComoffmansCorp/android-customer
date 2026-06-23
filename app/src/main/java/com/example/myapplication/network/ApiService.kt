package com.example.myapplication.network

import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    // Tasks
    @GET("api/tasks")
    suspend fun getTasks(
        @Query("status") status: String? = null
    ): List<TaskResponse>

    @PUT("api/tasks/{id}/status")
    suspend fun updateTaskStatus(
        @Path("id") id: Long,
        @Body request: TaskStatusUpdateRequest
    ): TaskResponse

    // Inspection acts
    @POST("api/acts/inspection")
    suspend fun createInspectionAct(@Body request: InspectionActRequest): InspectionActResponse

    @GET("api/acts/inspection")
    suspend fun getInspectionActs(): List<InspectionActResponse>

    // Replacement acts
    @POST("api/acts/replacement")
    suspend fun createReplacementAct(@Body request: ReplacementActRequest): ReplacementActResponse

    @GET("api/acts/replacement")
    suspend fun getReplacementActs(): List<ReplacementActResponse>

    // Dictionaries
    @GET("api/dictionaries/addresses")
    suspend fun getAddresses(@Query("q") query: String = ""): List<AddressResponse>
}
