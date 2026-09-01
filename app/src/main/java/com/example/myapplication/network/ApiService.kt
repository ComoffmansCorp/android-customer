package com.example.myapplication.network

import retrofit2.http.*

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    // ── Catalog (public) ──────────────────────────────────────────────────────
    @GET("api/catalog/categories")
    suspend fun getCategories(): List<CategoryResponse>

    @GET("api/catalog/services")
    suspend fun getServices(@Query("categoryId") categoryId: Long? = null): List<ServiceResponse>

    // ── Master profile ─────────────────────────────────────────────────────
    @GET("api/master/profile")
    suspend fun getMasterProfile(): MasterProfileResponse

    @PUT("api/master/profile")
    suspend fun updateMasterProfile(@Body request: UpdateMasterProfileRequest): MasterProfileResponse

    @GET("api/masters/{id}/reviews")
    suspend fun getMasterReviews(
        @Path("id") id: Long,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PageResponse<ReviewResponse>

    // ── Requests ────────────────────────────────────────────────────────────
    // Server-side filtered by the master's own specializations.
    @GET("api/requests/open")
    suspend fun getOpenRequests(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PageResponse<ServiceRequestResponse>

    // Requests ever assigned to this master (history/work list).
    @GET("api/requests")
    suspend fun getMyRequests(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PageResponse<ServiceRequestResponse>

    @GET("api/requests/{id}")
    suspend fun getServiceRequest(@Path("id") id: Long): ServiceRequestResponse

    // Bidding: the master proposes a price/comment instead of directly
    // claiming -- the client (web-only) picks one offer to accept.
    @POST("api/requests/{id}/offers")
    suspend fun submitOffer(@Path("id") id: Long, @Body request: SubmitOfferRequest): OfferResponse

    @POST("api/requests/{id}/complete")
    suspend fun completeRequest(@Path("id") id: Long): ServiceRequestResponse

    @POST("api/requests/{id}/cancel")
    suspend fun cancelRequest(@Path("id") id: Long, @Body request: CancelRequestRequest): ServiceRequestResponse

    @GET("api/requests/{id}/payment")
    suspend fun getPayment(@Path("id") id: Long): PaymentResponse

    // ── Chat ────────────────────────────────────────────────────────────────
    @GET("api/requests/{id}/messages")
    suspend fun getMessages(@Path("id") id: Long, @Query("sinceId") sinceId: Long? = null): List<MessageResponse>

    @POST("api/requests/{id}/messages")
    suspend fun sendMessage(@Path("id") id: Long, @Body request: SendMessageRequest): MessageResponse
}
