package com.example.mgc_keyboard.statscore

import androidx.room.Entity
import androidx.room.PrimaryKey

/** US7-2: immutable audit trail of consent changes and (would-be) data transfers. Insert-only, never updated or deleted. */
@Entity(tableName = "audit_log")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val eventType: String,
    val detail: String
)

object AuditEventType {
    const val ONBOARDING_CONSENT_ACCEPTED = "onboarding_consent_accepted"
    const val PERMISSION_GRANTED = "permission_granted"
    const val PERMISSION_DENIED = "permission_denied"
    const val PIN_SET = "pin_set"
    const val THRESHOLDS_CHANGED = "thresholds_changed"
    const val EXTERNAL_TRANSFER_BLOCKED = "external_transfer_blocked"
}
