package com.sys.audit5s.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audits")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val auditorName: String,
    val workshopRepName: String,
    val sectionName: String,
    val finalCategory: String? = null
)