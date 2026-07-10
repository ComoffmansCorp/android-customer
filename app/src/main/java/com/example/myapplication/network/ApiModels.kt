package com.example.myapplication.network

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("tenantCode") val tenantCode: String,
    @SerializedName("username")   val username: String,
    @SerializedName("password")   val password: String
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class AuthResponse(
    @SerializedName("accessToken")  val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn")    val expiresIn: Long,
    @SerializedName("fullName")     val fullName: String?,
    @SerializedName("role")         val role: String?,
    @SerializedName("tenantId")     val tenantId: Long?,
    @SerializedName("tenantCode")   val tenantCode: String?,
    @SerializedName("tenantName")   val tenantName: String?,
    @SerializedName("tenantPlan")   val tenantPlan: String?
)

// ── Pagination — mirrors internal/platform/httpx.Page[T] on the backend ───────

data class PageResponse<T>(
    @SerializedName("items")      val items: List<T>,
    @SerializedName("page")       val page: Int,
    @SerializedName("pageSize")   val pageSize: Int,
    @SerializedName("totalItems") val totalItems: Long,
    @SerializedName("totalPages") val totalPages: Int
)

// ── Tasks ─────────────────────────────────────────────────────────────────────

data class TaskResponse(
    @SerializedName("id")           val id: Long,
    @SerializedName("type")         val type: String,       // INSPECTION | REPLACEMENT
    @SerializedName("addressId")    val addressId: Long,
    @SerializedName("addressLabel") val addressLabel: String?,
    @SerializedName("status")       val status: String,     // PENDING | IN_PROGRESS | COMPLETED | CANCELED
    @SerializedName("dueDate")      val dueDate: String?,
    @SerializedName("assigneeId")   val assigneeId: Long?,
    @SerializedName("assigneeName") val assigneeName: String?,
    @SerializedName("completedAt")  val completedAt: String?,
    @SerializedName("cancelReason") val cancelReason: String?
)

// ── Addresses ─────────────────────────────────────────────────────────────────

data class AddressResponse(
    @SerializedName("id")           val id: Long,
    @SerializedName("street")       val street: String?,
    @SerializedName("house")        val house: String?,
    @SerializedName("building")     val building: String?,
    @SerializedName("apartment")    val apartment: String?,
    @SerializedName("consumerId")   val consumerId: Long?,
    @SerializedName("consumerName") val consumerName: String?
)

// ── Inspection act ────────────────────────────────────────────────────────────

data class InspectionActRequest(
    @SerializedName("taskId")         val taskId: Long,
    @SerializedName("inspectionDate") val inspectionDate: String? = null,
    @SerializedName("consumerId")     val consumerId: Long? = null,
    @SerializedName("inspectionType") val inspectionType: String,    // SCHEDULED | UNSCHEDULED
    @SerializedName("notes")          val notes: String? = null
)

data class InspectionActResponse(
    @SerializedName("id")             val id: Long,
    @SerializedName("taskId")         val taskId: Long,
    @SerializedName("addressId")      val addressId: Long,
    @SerializedName("addressLabel")   val addressLabel: String?,
    @SerializedName("inspectionDate") val inspectionDate: String?,
    @SerializedName("consumerId")     val consumerId: Long?,
    @SerializedName("consumerName")   val consumerName: String?,
    @SerializedName("inspectionType") val inspectionType: String?,
    @SerializedName("notes")          val notes: String?,
    @SerializedName("meterCount")     val meterCount: Int,
    @SerializedName("photoCount")     val photoCount: Int
)

// ── Meters (nested under an inspection act) ────────────────────────────────────

data class MeterRequest(
    @SerializedName("type")                val type: String,
    @SerializedName("serialNumber")        val serialNumber: String,
    @SerializedName("manufactureYear")     val manufactureYear: Int? = null,
    @SerializedName("verificationDate")    val verificationDate: String? = null,
    @SerializedName("sealState")           val sealState: String? = null,
    @SerializedName("transformationRatio") val transformationRatio: Int? = null
)

data class MeterResponse(
    @SerializedName("id")                  val id: Long,
    @SerializedName("type")                val type: String,
    @SerializedName("serialNumber")        val serialNumber: String,
    @SerializedName("manufactureYear")     val manufactureYear: Int?,
    @SerializedName("verificationDate")    val verificationDate: String?,
    @SerializedName("sealState")           val sealState: String?,
    @SerializedName("transformationRatio") val transformationRatio: Int?
)

// ── Replacement act ───────────────────────────────────────────────────────────

data class ReplacementActRequest(
    @SerializedName("taskId")           val taskId: Long,
    @SerializedName("accountNumber")    val accountNumber: String,
    @SerializedName("installationDate") val installationDate: String? = null,
    @SerializedName("oldBrand")         val oldBrand: String? = null,
    @SerializedName("oldSerialNumber")  val oldSerialNumber: String? = null,
    @SerializedName("oldReadings")      val oldReadings: Double? = null,
    @SerializedName("newBrand")         val newBrand: String? = null,
    @SerializedName("newSerialNumber")  val newSerialNumber: String? = null,
    @SerializedName("newReadings")      val newReadings: Double? = null
)

data class ReplacementActResponse(
    @SerializedName("id")               val id: Long,
    @SerializedName("taskId")           val taskId: Long,
    @SerializedName("addressId")        val addressId: Long,
    @SerializedName("addressLabel")     val addressLabel: String?,
    @SerializedName("accountNumber")    val accountNumber: String?,
    @SerializedName("installationDate") val installationDate: String?,
    @SerializedName("oldBrand")         val oldBrand: String?,
    @SerializedName("oldSerialNumber")  val oldSerialNumber: String?,
    @SerializedName("oldReadings")      val oldReadings: Double?,
    @SerializedName("newBrand")         val newBrand: String?,
    @SerializedName("newSerialNumber")  val newSerialNumber: String?,
    @SerializedName("newReadings")      val newReadings: Double?,
    @SerializedName("photoCount")       val photoCount: Int
)

// ── Photos ────────────────────────────────────────────────────────────────────

data class PhotoResponse(
    @SerializedName("id")               val id: Long,
    @SerializedName("note")             val note: String?,
    @SerializedName("originalFilename") val originalFilename: String?,
    @SerializedName("contentType")      val contentType: String?,
    @SerializedName("sizeBytes")        val sizeBytes: Long
)

// ── API error — mirrors internal/platform/httpx.Problem (RFC 7807) ────────────

data class ApiError(
    @SerializedName("title")  val title: String?,
    @SerializedName("detail") val detail: String?,
    @SerializedName("status") val status: Int?
)
