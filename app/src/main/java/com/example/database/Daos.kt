package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.models.ActivityLog
import com.example.models.AppSettingsEntity
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.PaymentRecord
import com.example.models.PaymentStatus
import com.example.models.ProjectStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY updatedDate DESC")
    fun getAllActiveCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerByIdFlow(id: String): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    suspend fun getCustomerByCustomId(customerId: String): Customer?

    @Query("""
        SELECT * FROM customers 
        WHERE isArchived = 0 
          AND (
            customerName LIKE '%' || :query || '%' 
            OR customerId LIKE '%' || :query || '%' 
            OR mobileNumber LIKE '%' || :query || '%' 
            OR whatsappNumber LIKE '%' || :query || '%' 
            OR businessName LIKE '%' || :query || '%' 
            OR city LIKE '%' || :query || '%'
          )
        ORDER BY updatedDate DESC
    """)
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("SELECT COUNT(*) FROM customers WHERE isArchived = 0")
    fun getActiveCustomerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getTotalRecordCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET isArchived = 1, updatedDate = :timestamp WHERE id = :id")
    suspend fun archiveCustomer(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isArchived = 0, updatedDate = :timestamp WHERE id = :id")
    suspend fun restoreCustomer(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerPermanently(id: String)

    @Query("SELECT * FROM customers WHERE isArchived = 1 ORDER BY updatedDate DESC")
    fun getArchivedCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isArchived = 0 AND followUpDate > 0 AND followUpDate < :currentTimestamp AND currentStatus != 'SUCCESS' AND currentStatus != 'WEBSITE_COMPLETED' AND currentStatus != 'CANCELLED'")
    suspend fun getOverdueCustomers(currentTimestamp: Long): List<Customer>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersRaw(): List<Customer>

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getLogsForCustomer(customerId: String): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLog)

    @Query("SELECT * FROM activity_logs")
    suspend fun getAllLogs(): List<ActivityLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<ActivityLog>)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord)

    @Query("SELECT * FROM payments")
    suspend fun getAllPayments(): List<PaymentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<PaymentRecord>)

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 'global_settings' LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'global_settings' LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettingsEntity)
}
