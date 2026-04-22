package com.sys.audit5s.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "remark_photos",
    foreignKeys = [ForeignKey(
        entity = RemarkEntity::class,
        parentColumns = ["id"],
        childColumns = ["remarkId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("remarkId")]
)
data class RemarkPhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remarkId: Long,
    val photoUri: String
)