package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.models.ActivityLog
import com.example.models.AppSettingsEntity
import com.example.models.Customer
import com.example.models.CustomerStatus
import com.example.models.PaymentRecord
import com.example.models.PaymentStatus
import com.example.models.PriorityLevel
import com.example.models.ProjectStatus

class Converters {
    @TypeConverter
    fun fromCustomerStatus(value: CustomerStatus): String = value.name

    @TypeConverter
    fun toCustomerStatus(value: String): CustomerStatus = runCatching {
        CustomerStatus.valueOf(value)
    }.getOrDefault(CustomerStatus.NEW_INQUIRY)

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = runCatching {
        PaymentStatus.valueOf(value)
    }.getOrDefault(PaymentStatus.PENDING)

    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = runCatching {
        ProjectStatus.valueOf(value)
    }.getOrDefault(ProjectStatus.NOT_STARTED)

    @TypeConverter
    fun fromPriorityLevel(value: PriorityLevel): String = value.name

    @TypeConverter
    fun toPriorityLevel(value: String): PriorityLevel = runCatching {
        PriorityLevel.valueOf(value)
    }.getOrDefault(PriorityLevel.MEDIUM)
}

@Database(
    entities = [
        Customer::class,
        ActivityLog::class,
        PaymentRecord::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun paymentDao(): PaymentDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "web_record_waqar.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
