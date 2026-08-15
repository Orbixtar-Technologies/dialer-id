package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallStatus {
    COMPLETED,
    CANCELLED,
    FAILED,
    NO_ANSWER,
    DISCONNECTED
}

@Entity(tableName = "caller_ids")
data class CallerIdItem(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val label: String,
    val isPrimary: Boolean = false,
    val isVerified: Boolean = false,
    val countryCode: String = "US",
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_logs")
data class CallLogItem(
    @PrimaryKey val id: String,
    val destinationNumber: String,
    val callerIdUsed: String,
    val countryName: String,
    val status: CallStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val billingRatePerMin: Double,
    val totalCost: Double
)

