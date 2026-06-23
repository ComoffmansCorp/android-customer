package com.example.myapplication.network

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("tenantCode") val tenantCode: String,
    @SerializedName("username")   val username: String,
    @SerializedName("password")   val password: String
)

data class AuthResponse(
    @SerializedName("token")      val token: String,
    @SerializedName("fullName")   val fullName: String?,
    @SerializedName("role")       val role: String?,
    @SerializedName("tenantId")   val tenantId: Long?,
    @SerializedName("tenantCode") val tenantCode: String?,
    @SerializedName("tenantName") val tenantName: String?
)

// ── Tasks ─────────────────────────────────────────────────────────────────────

data class TaskResponse(
    @SerializedName("id")           val id: Long,
    @SerializedName("type")         val type: String,       // INSPECTION | REPLACEMENT
    @SerializedName("address")      val address: String,
    @SerializedName("consumerName") val consumerName: String?,
    @SerializedName("status")       val status: String,     // PENDING | IN_PROGRESS | COMPLETED | CANCELED
    @SerializedName("dueDate")      val dueDate: String?
)

data class TaskStatusUpdateRequest(
    @SerializedName("status") val status: String
)

// ── Addresses ─────────────────────────────────────────────────────────────────

data class AddressResponse(
    @SerializedName("id")        val id: Long,
    @SerializedName("street")    val street: String?,
    @SerializedName("house")     val house: String?,
    @SerializedName("building")  val building: String?,
    @SerializedName("apartment") val apartment: String?,
    @SerializedName("consumer")  val consumer: String?
)

// ── Inspection Act ────────────────────────────────────────────────────────────

data class InspectionActRequest(
    @SerializedName("taskId")         val taskId: Long,
    @SerializedName("addressId")      val addressId: Long,
    @SerializedName("consumerId")     val consumerId: Long? = null,
    @SerializedName("inspectionDate") val inspectionDate: String? = null,
    @SerializedName("inspectionType") val inspectionType: String,    // SCHEDULED | UNSCHEDULED
    @SerializedName("notes")          val notes: String? = null,
    @SerializedName("meters")         val meters: List<MeterRequest>? = null
)

data class MeterRequest(
    @SerializedName("type")               val type: String,
    @SerializedName("serialNumber")       val serialNumber: String,
    @SerializedName("manufactureYear")    val manufactureYear: Int? = null,
    @SerializedName("verificationDate")   val verificationDate: String? = null,
    @SerializedName("sealState")          val sealState: String? = null,
    @SerializedName("transformationRatio") val transformationRatio: Int? = null
)

data class InspectionActResponse(
    @SerializedName("id")             val id: Long,
    @SerializedName("taskId")         val taskId: Long,
    @SerializedName("address")        val address: String?,
    @SerializedName("consumer")       val consumer: String?,
    @SerializedName("inspectionDate") val inspectionDate: String?,
    @SerializedName("inspectionType") val inspectionType: String?,
    @SerializedName("notes")          val notes: String?,
    @SerializedName("photosCount")    val photosCount: Int
)

// ── Replacement Act ───────────────────────────────────────────────────────────

data class ReplacementActRequest(
    @SerializedName("taskId")           val taskId: Long,
    @SerializedName("addressId")        val addressId: Long,
    @SerializedName("accountNumber")    val accountNumber: String,
    @SerializedName("installationDate") val installationDate: String? = null,
    @SerializedName("oldMeter")         val oldMeter: MeterInfoRequest,
    @SerializedName("newMeter")         val newMeter: MeterInfoRequest
)

data class MeterInfoRequest(
    @SerializedName("brand")        val brand: String,
    @SerializedName("serialNumber") val serialNumber: String,
    @SerializedName("readings")     val readings: Double
)

data class ReplacementActResponse(
    @SerializedName("id")               val id: Long,
    @SerializedName("taskId")           val taskId: Long,
    @SerializedName("address")          val address: String?,
    @SerializedName("accountNumber")    val accountNumber: String?,
    @SerializedName("installationDate") val installationDate: String?
)

// ── API error ─────────────────────────────────────────────────────────────────

data class ApiError(
    @SerializedName("message") val message: String?,
    @SerializedName("error")   val error: String?
)
