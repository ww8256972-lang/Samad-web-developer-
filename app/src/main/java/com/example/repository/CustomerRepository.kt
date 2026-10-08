package com.example.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.database.AppDatabase
import com.example.models.ActivityLog
import com.example.models.AppSettingsEntity
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.DashboardSummary
import com.example.models.PaymentRecord
import com.example.models.PaymentStatus
import com.example.models.PriorityLevel
import com.example.models.ProjectStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class CustomerRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val customerDao = database.customerDao()
    private val logDao = database.activityLogDao()
    private val paymentDao = database.paymentDao()
    private val settingsDao = database.settingsDao()

    val allActiveCustomers: Flow<List<Customer>> = customerDao.getAllActiveCustomers()
    val archivedCustomers: Flow<List<Customer>> = customerDao.getArchivedCustomers()
    val totalCustomerCount: Flow<Int> = customerDao.getActiveCustomerCount()
    val appSettingsFlow: Flow<AppSettingsEntity?> = settingsDao.getSettingsFlow()

    fun getCustomerByIdFlow(id: String): Flow<Customer?> = customerDao.getCustomerByIdFlow(id)

    fun getLogsForCustomer(customerId: String): Flow<List<ActivityLog>> = logDao.getLogsForCustomer(customerId)

    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentRecord>> = paymentDao.getPaymentsForCustomer(customerId)

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)

    val dashboardSummary: Flow<DashboardSummary> = customerDao.getAllActiveCustomers().map { list ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfToday = cal.timeInMillis

        var newInquiries = 0
        var followupsToday = 0
        var overdueFollowups = 0
        var confirmedProjects = 0
        var inProgressProjects = 0
        var completedProjects = 0
        var pendingPaymentsCount = 0
        var paymentsReceivedCount = 0
        var totalRev = 0.0
        var totalPending = 0.0
        var totalConfirm = 0.0
        var totalSuccess = 0.0

        for (cust in list) {
            if (cust.currentStatus == CustomerStatus.NEW_INQUIRY) newInquiries++
            if (cust.currentStatus == CustomerStatus.CONFIRMED) confirmedProjects++
            if (cust.currentStatus == CustomerStatus.IN_PROGRESS) inProgressProjects++
            if (cust.currentStatus == CustomerStatus.WEBSITE_COMPLETED) completedProjects++
            if (cust.paymentStatus == PaymentStatus.PENDING || cust.pendingAmount > 0) pendingPaymentsCount++
            if (cust.paymentStatus == PaymentStatus.RECEIVED || cust.currentStatus == CustomerStatus.PAYMENT_RECEIVED) paymentsReceivedCount++

            totalRev += cust.paymentReceived
            totalPending += cust.pendingAmount
            totalConfirm += cust.confirmAmount
            totalSuccess += cust.successAmount

            if (cust.followUpDate in startOfToday until endOfToday &&
                cust.currentStatus != CustomerStatus.SUCCESS &&
                cust.currentStatus != CustomerStatus.CANCELLED
            ) {
                followupsToday++
            }
            if (cust.followUpDate > 0 && cust.followUpDate < startOfToday &&
                cust.currentStatus != CustomerStatus.SUCCESS &&
                cust.currentStatus != CustomerStatus.CANCELLED &&
                cust.currentStatus != CustomerStatus.WEBSITE_COMPLETED
            ) {
                overdueFollowups++
            }
        }

        DashboardSummary(
            totalCustomers = list.size,
            newInquiries = newInquiries,
            followupsToday = followupsToday,
            overdueFollowups = overdueFollowups,
            confirmedProjects = confirmedProjects,
            inProgressProjects = inProgressProjects,
            completedProjects = completedProjects,
            pendingPaymentsCount = pendingPaymentsCount,
            paymentsReceivedCount = paymentsReceivedCount,
            totalRevenue = totalRev,
            totalPendingRevenue = totalPending,
            totalConfirmAmount = totalConfirm,
            totalSuccessAmount = totalSuccess
        )
    }

    suspend fun createCustomer(customer: Customer): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Check uniqueness of customerId
            val existing = customerDao.getCustomerByCustomId(customer.customerId)
            if (existing != null) {
                throw IllegalArgumentException("Customer ID '${customer.customerId}' already exists! Please use a unique ID.")
            }

            database.withTransaction {
                customerDao.insertCustomer(customer)
                logDao.insertLog(
                    ActivityLog(
                        customerId = customer.id,
                        title = "Customer Created",
                        description = "New customer record created for ${customer.customerName} (${customer.businessName})",
                        category = "STATUS"
                    )
                )
            }
        }
    }

    suspend fun updateCustomer(customer: Customer, changeSummary: String = "Record updated"): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                val updated = customer.copy(updatedDate = System.currentTimeMillis())
                customerDao.updateCustomer(updated)
                logDao.insertLog(
                    ActivityLog(
                        customerId = customer.id,
                        title = "Customer Updated",
                        description = changeSummary,
                        category = "STATUS"
                    )
                )
            }
        }
    }

    suspend fun recordPayment(
        customer: Customer,
        amount: Double,
        paymentMethod: String,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                val newReceived = customer.paymentReceived + amount
                val newPending = (customer.confirmAmount - newReceived).coerceAtLeast(0.0)
                val newPaymentStatus = if (newPending <= 0.0 && customer.confirmAmount > 0.0) {
                    PaymentStatus.RECEIVED
                } else {
                    PaymentStatus.PARTIAL
                }

                val newCustomerStatus = if (newPaymentStatus == PaymentStatus.RECEIVED) {
                    if (customer.projectStatus == ProjectStatus.COMPLETED) {
                        CustomerStatus.SUCCESS
                    } else {
                        CustomerStatus.PAYMENT_RECEIVED
                    }
                } else customer.currentStatus

                val newProjectStatus = if (newCustomerStatus == CustomerStatus.SUCCESS) {
                    ProjectStatus.SUCCESS
                } else customer.projectStatus

                val newSuccessAmount = if (newCustomerStatus == CustomerStatus.SUCCESS) {
                    customer.confirmAmount
                } else customer.successAmount

                val updatedCustomer = customer.copy(
                    paymentReceived = newReceived,
                    pendingAmount = newPending,
                    paymentStatus = newPaymentStatus,
                    currentStatus = newCustomerStatus,
                    projectStatus = newProjectStatus,
                    successAmount = newSuccessAmount,
                    updatedDate = System.currentTimeMillis()
                )

                customerDao.updateCustomer(updatedCustomer)
                paymentDao.insertPayment(
                    PaymentRecord(
                        customerId = customer.id,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        note = note,
                        paymentDate = System.currentTimeMillis()
                    )
                )
                logDao.insertLog(
                    ActivityLog(
                        customerId = customer.id,
                        title = "Payment Received: ₹$amount",
                        description = "Payment of ₹$amount via $paymentMethod. Balance: ₹$newPending. Status: ${newPaymentStatus.displayName}",
                        category = "PAYMENT"
                    )
                )
            }
        }
    }

    suspend fun markProjectSuccess(customer: Customer): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            database.withTransaction {
                val updated = customer.copy(
                    currentStatus = CustomerStatus.SUCCESS,
                    projectStatus = ProjectStatus.SUCCESS,
                    paymentStatus = PaymentStatus.RECEIVED,
                    paymentReceived = if (customer.confirmAmount > 0) customer.confirmAmount else customer.paymentReceived,
                    pendingAmount = 0.0,
                    successAmount = if (customer.confirmAmount > 0) customer.confirmAmount else customer.estimatedProjectAmount,
                    updatedDate = System.currentTimeMillis()
                )
                customerDao.updateCustomer(updated)
                logDao.insertLog(
                    ActivityLog(
                        customerId = customer.id,
                        title = "Project Marked SUCCESS",
                        description = "Project successfully finalized! Payment status set to Received, Project marked Completed & Success.",
                        category = "STATUS"
                    )
                )
            }
        }
    }

    suspend fun logCall(customer: Customer): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val updated = customer.copy(lastCallTimestamp = now, updatedDate = now)
            customerDao.updateCustomer(updated)
            logDao.insertLog(
                ActivityLog(
                    customerId = customer.id,
                    title = "Phone Call Placed",
                    description = "Dialer opened for mobile number ${customer.mobileNumber}",
                    category = "CALL",
                    timestamp = now
                )
            )
        }
    }

    suspend fun logWhatsappMessage(customer: Customer, messageContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val updated = customer.copy(lastWhatsappTimestamp = now, updatedDate = now)
            customerDao.updateCustomer(updated)
            logDao.insertLog(
                ActivityLog(
                    customerId = customer.id,
                    title = "WhatsApp Reminder Sent",
                    description = messageContent.take(120),
                    category = "WHATSAPP",
                    timestamp = now
                )
            )
        }
    }

    suspend fun archiveCustomer(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            customerDao.archiveCustomer(id)
            logDao.insertLog(
                ActivityLog(
                    customerId = id,
                    title = "Customer Archived",
                    description = "Customer record was archived.",
                    category = "STATUS"
                )
            )
        }
    }

    suspend fun restoreCustomer(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            customerDao.restoreCustomer(id)
            logDao.insertLog(
                ActivityLog(
                    customerId = id,
                    title = "Customer Restored",
                    description = "Customer record was restored from archive.",
                    category = "STATUS"
                )
            )
        }
    }

    suspend fun deletePermanently(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            customerDao.deleteCustomerPermanently(id)
        }
    }

    suspend fun getAppSettings(): AppSettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettings() ?: AppSettingsEntity().also {
            settingsDao.saveSettings(it)
        }
    }

    suspend fun saveAppSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }

    suspend fun generateNextCustomerId(): String = withContext(Dispatchers.IO) {
        val total = customerDao.getTotalRecordCount()
        "CUST-${1001 + total}"
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = customerDao.getTotalRecordCount()
        if (count == 0) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            val sampleCustomers = listOf(
                Customer(
                    customerId = "CUST-1001",
                    customerName = "Ahmad Raza",
                    mobileNumber = "+923001234567",
                    whatsappNumber = "+923001234567",
                    businessName = "Apex Real Estate Solutions",
                    businessType = "Real Estate",
                    city = "Lahore",
                    state = "Punjab",
                    websiteRequirement = "Property Listing Portal with Agent CRM & WhatsApp lead buttons",
                    websiteType = "Real Estate Portal",
                    requiredFeatures = "Search Filters, Virtual Tours, Admin Dashboard, Payment Gateway",
                    estimatedProjectAmount = 75000.0,
                    confirmAmount = 70000.0,
                    pendingAmount = 25000.0,
                    paymentReceived = 45000.0,
                    currentStatus = CustomerStatus.IN_PROGRESS,
                    paymentStatus = PaymentStatus.PARTIAL,
                    projectStatus = ProjectStatus.IN_PROGRESS,
                    priority = PriorityLevel.HIGH,
                    followUpDate = now + dayMillis * 2,
                    inquiryDate = now - dayMillis * 10,
                    notes = "Client requested modern cyber-dark style with fast loading speed."
                ),
                Customer(
                    customerId = "CUST-1002",
                    customerName = "Tariq Mahmood",
                    mobileNumber = "+923219876543",
                    whatsappNumber = "+923219876543",
                    businessName = "Mahmood Fabrics & Textiles",
                    businessType = "E-Commerce / Retail",
                    city = "Faisalabad",
                    state = "Punjab",
                    websiteRequirement = "Full multi-vendor WooCommerce store with inventory synchronization",
                    websiteType = "E-Commerce",
                    requiredFeatures = "Cart, EasyPaisa/JazzCash, Order Tracking, Customer Accounts",
                    estimatedProjectAmount = 120000.0,
                    confirmAmount = 115000.0,
                    pendingAmount = 0.0,
                    successAmount = 115000.0,
                    paymentReceived = 115000.0,
                    currentStatus = CustomerStatus.SUCCESS,
                    paymentStatus = PaymentStatus.RECEIVED,
                    projectStatus = ProjectStatus.SUCCESS,
                    priority = PriorityLevel.URGENT,
                    followUpDate = 0L,
                    inquiryDate = now - dayMillis * 20,
                    notes = "Project delivered with 100% client satisfaction and full advance paid."
                ),
                Customer(
                    customerId = "CUST-1003",
                    customerName = "Dr. Sameer Khan",
                    mobileNumber = "+923335551212",
                    whatsappNumber = "+923335551212",
                    businessName = "Apex Dental Clinic",
                    businessType = "Healthcare / Clinic",
                    city = "Islamabad",
                    state = "Federal",
                    websiteRequirement = "Online Patient Appointment Booking & Doctor Profile System",
                    websiteType = "Medical Portal",
                    requiredFeatures = "Calendar Booking, SMS/WhatsApp Confirmations, Patient Records",
                    estimatedProjectAmount = 55000.0,
                    confirmAmount = 50000.0,
                    pendingAmount = 50000.0,
                    paymentReceived = 0.0,
                    currentStatus = CustomerStatus.FOLLOW_UP,
                    paymentStatus = PaymentStatus.PENDING,
                    projectStatus = ProjectStatus.NOT_STARTED,
                    priority = PriorityLevel.HIGH,
                    followUpDate = now - dayMillis * 2, // OVERDUE!
                    inquiryDate = now - dayMillis * 5,
                    notes = "Scheduled demo call. Reminder message needed on WhatsApp."
                ),
                Customer(
                    customerId = "CUST-1004",
                    customerName = "Bilal Siddiqui",
                    mobileNumber = "+923451122334",
                    whatsappNumber = "+923451122334",
                    businessName = "Karachi Auto Parts Hub",
                    businessType = "Automotive / Parts",
                    city = "Karachi",
                    state = "Sindh",
                    websiteRequirement = "Online Catalog with Spare Part search by Model & Year",
                    websiteType = "Product Catalog",
                    requiredFeatures = "Catalog filtering, Quotation generator, PDF export",
                    estimatedProjectAmount = 60000.0,
                    confirmAmount = 60000.0,
                    pendingAmount = 30000.0,
                    paymentReceived = 30000.0,
                    currentStatus = CustomerStatus.CONFIRMED,
                    paymentStatus = PaymentStatus.PARTIAL,
                    projectStatus = ProjectStatus.IN_PROGRESS,
                    priority = PriorityLevel.MEDIUM,
                    followUpDate = now, // Today!
                    inquiryDate = now - dayMillis * 2,
                    notes = "Advance payment received. Design wireframe approval in progress."
                )
            )

            customerDao.insertAll(sampleCustomers)
            for (c in sampleCustomers) {
                logDao.insertLog(
                    ActivityLog(
                        customerId = c.id,
                        title = "Initial Record Created",
                        description = "System initialized sample inquiry record for ${c.customerName}",
                        category = "STATUS"
                    )
                )
                if (c.paymentReceived > 0) {
                    paymentDao.insertPayment(
                        PaymentRecord(
                            customerId = c.id,
                            amount = c.paymentReceived,
                            paymentMethod = "Bank Wire",
                            note = "Initial deposit",
                            paymentDate = now - dayMillis * 2
                        )
                    )
                }
            }
        }
    }

    // Backup & Restore
    suspend fun exportDatabaseJson(): String = withContext(Dispatchers.IO) {
        val customers = customerDao.getAllCustomersRaw()
        val logs = logDao.getAllLogs()
        val payments = paymentDao.getAllPayments()

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val backupMap = mapOf(
            "app" to "Web Record by Waqar",
            "version" to "1.0",
            "timestamp" to System.currentTimeMillis(),
            "customersCount" to customers.size,
            "customers" to customers,
            "logs" to logs,
            "payments" to payments
        )

        val adapter = moshi.adapter(Map::class.java)
        adapter.toJson(backupMap)
    }

    suspend fun restoreDatabaseFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any>>(type)
            val parsed = adapter.fromJson(jsonString) ?: throw IllegalArgumentException("Invalid backup JSON")

            // Re-parse customers
            val custJsonAdapter = moshi.adapter<List<Customer>>(Types.newParameterizedType(List::class.java, Customer::class.java))
            val logsJsonAdapter = moshi.adapter<List<ActivityLog>>(Types.newParameterizedType(List::class.java, ActivityLog::class.java))
            val payJsonAdapter = moshi.adapter<List<PaymentRecord>>(Types.newParameterizedType(List::class.java, PaymentRecord::class.java))

            val custJson = moshi.adapter(Any::class.java).toJson(parsed["customers"])
            val logsJson = moshi.adapter(Any::class.java).toJson(parsed["logs"])
            val payJson = moshi.adapter(Any::class.java).toJson(parsed["payments"])

            val customers = custJsonAdapter.fromJson(custJson) ?: emptyList()
            val logs = logsJsonAdapter.fromJson(logsJson) ?: emptyList()
            val payments = payJsonAdapter.fromJson(payJson) ?: emptyList()

            database.withTransaction {
                customerDao.clearAllCustomers()
                logDao.clearAllLogs()
                paymentDao.clearAllPayments()

                customerDao.insertAll(customers)
                logDao.insertAllLogs(logs)
                paymentDao.insertAllPayments(payments)
            }
            customers.size
        }
    }
}
