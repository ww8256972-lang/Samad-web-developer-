package com.example.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerId"], unique = true),
        Index(value = ["customerName"]),
        Index(value = ["mobileNumber"]),
        Index(value = ["businessName"]),
        Index(value = ["currentStatus"]),
        Index(value = ["followUpDate"]),
        Index(value = ["isArchived"])
    ]
)
data class Customer(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String, // e.g. "CUST-1001"
    val customerName: String,
    val mobileNumber: String,
    val whatsappNumber: String,
    val businessName: String,
    val businessType: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    
    // Website & Requirement
    val websiteRequirement: String = "",
    val websiteType: String = "Corporate / Business",
    val requiredFeatures: String = "",
    
    // Financials
    val estimatedProjectAmount: Double = 0.0,
    val confirmAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val successAmount: Double = 0.0,
    val paymentReceived: Double = 0.0,
    
    // Dates (Stored as epoch millis)
    val inquiryDate: Long = System.currentTimeMillis(),
    val followUpDate: Long = 0L,
    val expectedCompletionDate: Long = 0L,
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    
    // Status & Tracking
    val currentStatus: CustomerStatus = CustomerStatus.NEW_INQUIRY,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val projectStatus: ProjectStatus = ProjectStatus.NOT_STARTED,
    val priority: PriorityLevel = PriorityLevel.MEDIUM,
    
    // Notes & Meta
    val notes: String = "",
    val internalRemarks: String = "",
    val assignedAdmin: String = "waqar",
    val isArchived: Boolean = false,
    val lastCallTimestamp: Long = 0L,
    val lastWhatsappTimestamp: Long = 0L,
    val isSyncPending: Boolean = false
)

enum class CustomerStatus(val displayName: String) {
    NEW_INQUIRY("New Inquiry"),
    FOLLOW_UP("Follow Up"),
    INTERESTED("Interested"),
    NOT_INTERESTED("Not Interested"),
    CONFIRMED("Confirmed"),
    IN_PROGRESS("In Progress"),
    PAYMENT_PENDING("Payment Pending"),
    PAYMENT_RECEIVED("Payment Received"),
    WEBSITE_COMPLETED("Website Completed"),
    SUCCESS("Success"),
    CANCELLED("Cancelled")
}

enum class PaymentStatus(val displayName: String) {
    PENDING("Payment Pending"),
    PARTIAL("Partial Payment"),
    RECEIVED("Payment Received"),
    REFUNDED("Refunded")
}

enum class ProjectStatus(val displayName: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    TESTING("In Review / Testing"),
    COMPLETED("Website Completed"),
    SUCCESS("Success"),
    CANCELLED("Cancelled")
}

enum class PriorityLevel(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

@Entity(
    tableName = "activity_logs",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"])
    ]
)
data class ActivityLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val title: String,
    val description: String,
    val category: String = "GENERAL", // STATUS, PAYMENT, CALL, WHATSAPP, FOLLOWUP, NOTE
    val timestamp: Long = System.currentTimeMillis(),
    val author: String = "waqar"
)

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["paymentDate"])
    ]
)
data class PaymentRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val amount: Double,
    val paymentMethod: String = "Online / Bank Transfer",
    val transactionRef: String = "",
    val note: String = "",
    val paymentDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: String = "global_settings",
    val whatsappApiEndpoint: String = "",
    val whatsappApiKey: String = "",
    val whatsappTemplate: String = "Hello {customerName},\n\nThis is a reminder regarding your website inquiry ({projectName}).\nYour scheduled follow-up date has passed. Please contact us when convenient.\n\nThank you,\nWeb Record by Waqar",
    val autoSendWhatsappOverdue: Boolean = false,
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val enableNotifications: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val adminUsername: String = "waqar",
    val adminPasswordHash: String = "waqar" // stored password
)

data class DashboardSummary(
    val totalCustomers: Int = 0,
    val newInquiries: Int = 0,
    val followupsToday: Int = 0,
    val overdueFollowups: Int = 0,
    val confirmedProjects: Int = 0,
    val inProgressProjects: Int = 0,
    val completedProjects: Int = 0,
    val pendingPaymentsCount: Int = 0,
    val paymentsReceivedCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalPendingRevenue: Double = 0.0,
    val totalConfirmAmount: Double = 0.0,
    val totalSuccessAmount: Double = 0.0
)
