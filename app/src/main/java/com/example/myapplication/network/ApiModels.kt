package com.example.myapplication.network

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("accessToken")  val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn")    val expiresIn: Long,
    @SerializedName("userId")       val userId: Long,
    @SerializedName("fullName")     val fullName: String?,
    @SerializedName("role")         val role: String?
)

// ── Pagination — mirrors internal/platform/httpx.Page[T] on the backend ───────

data class PageResponse<T>(
    @SerializedName("items")      val items: List<T>,
    @SerializedName("page")       val page: Int,
    @SerializedName("pageSize")   val pageSize: Int,
    @SerializedName("totalItems") val totalItems: Long,
    @SerializedName("totalPages") val totalPages: Int
)

// ── API error — mirrors internal/platform/httpx.Problem (RFC 7807) ────────────

data class ApiError(
    @SerializedName("title")  val title: String?,
    @SerializedName("detail") val detail: String?,
    @SerializedName("status") val status: Int?
)

// ── Catalog — mirrors internal/catalog/dto.go ──────────────────────────────────

data class CategoryResponse(
    @SerializedName("id")            val id: Long,
    @SerializedName("name")          val name: String,
    @SerializedName("active")        val active: Boolean,
    @SerializedName("subcategories") val subcategories: List<CategoryResponse>? = null
)

data class ServiceResponse(
    @SerializedName("id")          val id: Long,
    @SerializedName("categoryId")  val categoryId: Long,
    @SerializedName("name")        val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("priceFrom")   val priceFrom: Double?,
    @SerializedName("priceTo")     val priceTo: Double?,
    @SerializedName("unit")        val unit: String?,
    @SerializedName("active")      val active: Boolean
)

// ── Master — mirrors internal/master/dto.go ────────────────────────────────────

data class MasterProfileResponse(
    @SerializedName("userId")            val userId: Long,
    @SerializedName("fullName")          val fullName: String?,
    @SerializedName("city")              val city: String?,
    @SerializedName("bio")               val bio: String?,
    @SerializedName("avatarUrl")         val avatarUrl: String?,
    @SerializedName("ratingAvg")         val ratingAvg: Double,
    @SerializedName("ratingCount")       val ratingCount: Int,
    @SerializedName("specializationIds") val specializationIds: List<Long>
)

data class UpdateMasterProfileRequest(
    @SerializedName("avatarUrl")         val avatarUrl: String?,
    @SerializedName("city")              val city: String,
    @SerializedName("bio")               val bio: String,
    @SerializedName("specializationIds") val specializationIds: List<Long>
)

// ── Requests / offers — mirrors internal/request/dto.go ───────────────────────

data class StatusHistoryEntryResponse(
    @SerializedName("fromStatus") val fromStatus: String?,
    @SerializedName("toStatus")   val toStatus: String,
    @SerializedName("changedBy")  val changedBy: Long,
    @SerializedName("comment")    val comment: String?,
    @SerializedName("createdAt")  val createdAt: String
)

data class ServiceRequestResponse(
    @SerializedName("id")           val id: Long,
    @SerializedName("serviceId")    val serviceId: Long,
    @SerializedName("serviceName")  val serviceName: String?,
    @SerializedName("description")  val description: String,
    @SerializedName("addressText")  val addressText: String,
    @SerializedName("latitude")     val latitude: Double?,
    @SerializedName("longitude")    val longitude: Double?,
    @SerializedName("status")       val status: String,     // OPEN | ASSIGNED | COMPLETED | CANCELED
    @SerializedName("clientId")     val clientId: Long,
    @SerializedName("masterId")     val masterId: Long?,
    @SerializedName("agreedPrice")  val agreedPrice: Double?,
    @SerializedName("cancelReason") val cancelReason: String?,
    @SerializedName("createdAt")    val createdAt: String,
    @SerializedName("updatedAt")    val updatedAt: String,
    @SerializedName("history")      val history: List<StatusHistoryEntryResponse>? = null
)

data class SubmitOfferRequest(
    @SerializedName("price")   val price: Double,
    @SerializedName("comment") val comment: String
)

data class OfferResponse(
    @SerializedName("id")        val id: Long,
    @SerializedName("requestId") val requestId: Long,
    @SerializedName("masterId")  val masterId: Long,
    @SerializedName("price")     val price: Double,
    @SerializedName("comment")   val comment: String?,
    @SerializedName("status")    val status: String,     // PENDING | ACCEPTED | REJECTED | WITHDRAWN
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class CancelRequestRequest(
    @SerializedName("reason") val reason: String
)

// ── Reviews — mirrors internal/review/dto.go ───────────────────────────────────

data class ReviewResponse(
    @SerializedName("id")        val id: Long,
    @SerializedName("requestId") val requestId: Long,
    @SerializedName("clientId")  val clientId: Long,
    @SerializedName("masterId")  val masterId: Long,
    @SerializedName("rating")    val rating: Int,
    @SerializedName("comment")   val comment: String?,
    @SerializedName("createdAt") val createdAt: String
)

// ── Payments — mirrors internal/payment/dto.go ─────────────────────────────────

data class PaymentResponse(
    @SerializedName("id")          val id: Long,
    @SerializedName("requestId")   val requestId: Long,
    @SerializedName("amount")      val amount: Double,
    @SerializedName("platformFee") val platformFee: Double,
    @SerializedName("status")      val status: String,     // HELD | RELEASED | REFUNDED
    @SerializedName("createdAt")   val createdAt: String,
    @SerializedName("updatedAt")   val updatedAt: String
)

// ── Chat — mirrors internal/chat/dto.go ────────────────────────────────────────

data class MessageResponse(
    @SerializedName("id")        val id: Long,
    @SerializedName("requestId") val requestId: Long,
    @SerializedName("senderId")  val senderId: Long,
    @SerializedName("text")      val text: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("readAt")    val readAt: String?
)

data class SendMessageRequest(
    @SerializedName("text") val text: String
)
