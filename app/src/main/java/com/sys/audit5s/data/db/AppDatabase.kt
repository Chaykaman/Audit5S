package com.sys.audit5s.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sys.audit5s.data.db.dao.AuditDao
import com.sys.audit5s.data.db.dao.QuestionScoreDao
import com.sys.audit5s.data.db.dao.RemarkDao
import com.sys.audit5s.data.db.dao.RemarkPhotoDao
import com.sys.audit5s.data.db.entity.AuditEntity
import com.sys.audit5s.data.db.entity.QuestionScoreEntity
import com.sys.audit5s.data.db.entity.RemarkEntity
import com.sys.audit5s.data.db.entity.RemarkPhotoEntity

@Database(
    entities = [
        AuditEntity::class,
        QuestionScoreEntity::class,
        RemarkEntity::class,
        RemarkPhotoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun questionScoreDao(): QuestionScoreDao
    abstract fun remarkDao(): RemarkDao
    abstract fun remarkPhotoDao(): RemarkPhotoDao
}