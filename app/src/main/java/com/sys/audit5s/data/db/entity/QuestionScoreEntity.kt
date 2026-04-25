package com.sys.audit5s.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_scores",
    foreignKeys = [ForeignKey(
        entity = AuditEntity::class,
        parentColumns = ["id"],
        childColumns = ["auditId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("auditId")]
)
data class QuestionScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val auditId: Long,
    val category: String,
    val questionNumber: Int,
    val questionText: String,
    val score: Int
)