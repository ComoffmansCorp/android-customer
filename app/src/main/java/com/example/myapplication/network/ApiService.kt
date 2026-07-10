package com.example.myapplication.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    // ── Tasks ───────────────────────────────────────────────────────────────
    // The backend scopes results to the caller's own tasks for the
    // ELECTRICIAN role, so no assigneeId filter is needed here.
    @GET("api/tasks")
    suspend fun getTasks(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PageResponse<TaskResponse>

    @GET("api/tasks/{id}")
    suspend fun getTask(@Path("id") id: Long): TaskResponse

    @POST("api/tasks/{id}/start")
    suspend fun startTask(@Path("id") id: Long): TaskResponse

    @POST("api/tasks/{id}/complete")
    suspend fun completeTask(@Path("id") id: Long): TaskResponse

    // ── Inspection acts ─────────────────────────────────────────────────────
    @POST("api/acts/inspection")
    suspend fun createInspectionAct(@Body request: InspectionActRequest): InspectionActResponse

    @GET("api/acts/inspection/{id}")
    suspend fun getInspectionAct(@Path("id") id: Long): InspectionActResponse

    @GET("api/acts/inspection/by-task/{taskId}")
    suspend fun getInspectionActByTask(@Path("taskId") taskId: Long): InspectionActResponse

    @GET("api/acts/inspection/{id}/meters")
    suspend fun getMeters(@Path("id") actId: Long): List<MeterResponse>

    @POST("api/acts/inspection/{id}/meters")
    suspend fun addMeter(@Path("id") actId: Long, @Body request: MeterRequest): MeterResponse

    @DELETE("api/acts/inspection/{id}/meters/{meterId}")
    suspend fun deleteMeter(@Path("id") actId: Long, @Path("meterId") meterId: Long)

    @Streaming
    @GET("api/acts/inspection/{id}/pdf")
    suspend fun downloadInspectionPdf(@Path("id") id: Long): ResponseBody

    // ── Replacement acts ────────────────────────────────────────────────────
    @POST("api/acts/replacement")
    suspend fun createReplacementAct(@Body request: ReplacementActRequest): ReplacementActResponse

    @GET("api/acts/replacement/{id}")
    suspend fun getReplacementAct(@Path("id") id: Long): ReplacementActResponse

    @GET("api/acts/replacement/by-task/{taskId}")
    suspend fun getReplacementActByTask(@Path("taskId") taskId: Long): ReplacementActResponse

    @Streaming
    @GET("api/acts/replacement/{id}/pdf")
    suspend fun downloadReplacementPdf(@Path("id") id: Long): ResponseBody

    // ── Photos ──────────────────────────────────────────────────────────────
    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Part("inspectionActId") inspectionActId: RequestBody? = null,
        @Part("replacementActId") replacementActId: RequestBody? = null,
        @Part("note") note: RequestBody? = null
    ): PhotoResponse

    @GET("api/photos/inspection/{actId}")
    suspend fun getPhotosForInspection(@Path("actId") actId: Long): List<PhotoResponse>

    @GET("api/photos/replacement/{actId}")
    suspend fun getPhotosForReplacement(@Path("actId") actId: Long): List<PhotoResponse>

    @Streaming
    @GET("api/photos/{id}")
    suspend fun downloadPhoto(@Path("id") id: Long): ResponseBody

    @DELETE("api/photos/{id}")
    suspend fun deletePhoto(@Path("id") id: Long)

    // ── Addresses (reference/dictionary lookup) ─────────────────────────────
    @GET("api/addresses")
    suspend fun getAddresses(
        @Query("search") search: String = "",
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): PageResponse<AddressResponse>
}
