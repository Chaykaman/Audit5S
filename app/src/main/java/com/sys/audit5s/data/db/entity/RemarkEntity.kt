package com.sys.audit5s.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "remarks",
    foreignKeys = [ForeignKey(
        entity = QuestionScoreEntity::class,
        parentColumns = ["id"],
        childColumns = ["questionScoreId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("questionScoreId")]
)
data class RemarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionScoreId: Long,
    val auditId: Long,
    val deadlineDate: String,
    val actualDate: String? = null,
    val executorName: String,
    val description: String
)