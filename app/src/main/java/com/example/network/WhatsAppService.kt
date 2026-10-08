package com.example.network

import com.example.models.Customer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class WhatsAppMessagePayload(
    val recipientNumber: String,
    val messageText: String,
    val templateVariables: Map<String, String> = emptyMap()
)

data class WhatsAppApiResult(
    val success: Boolean,
    val message: String
)

object WhatsAppService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun formatTemplate(template: String, customer: Customer, adminName: String = "Waqar"): String {
        return template
            .replace("{customerName}", customer.customerName)
            .replace("{businessName}", customer.businessName)
            .replace("{projectName}", customer.websiteRequirement.ifBlank { customer.websiteType })
            .replace("{pendingAmount}", String.format("%.0f", customer.pendingAmount))
            .replace("{adminName}", adminName)
    }

    suspend fun sendWhatsAppViaApi(
        endpointUrl: String,
        apiKey: String,
        customer: Customer,
        messageText: String
    ): WhatsAppApiResult = withContext(Dispatchers.IO) {
        if (endpointUrl.isBlank()) {
            return@withContext WhatsAppApiResult(
                success = false,
                message = "WhatsApp API Endpoint is not configured in Settings. Please set endpoint or use direct WhatsApp app opener."
            )
        }

        try {
            val payload = mapOf(
                "phone" to customer.whatsappNumber.replace("+", "").replace(" ", "").replace("-", ""),
                "message" to messageText,
                "customer_id" to customer.customerId,
                "customer_name" to customer.customerName
            )
            val json = moshi.adapter(Map::class.java).toJson(payload)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

            val requestBuilder = Request.Builder()
                .url(endpointUrl)
                .post(body)

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                WhatsAppApiResult(true, "WhatsApp reminder sent successfully via API.")
            } else {
                WhatsAppApiResult(false, "API returned HTTP ${response.code}: ${response.message}")
            }
        } catch (e: Exception) {
            WhatsAppApiResult(false, "Connection error: ${e.localizedMessage}")
        }
    }
}
