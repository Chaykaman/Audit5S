package com.sys.audit5s.data.db.dao

import androidx.room.*
import com.sys.audit5s.data.db.entity.RemarkEntity

@Dao
interface RemarkDao {

    @Insert
    suspend fun insert(remark: RemarkEntity): Long

    @Update
    suspend fun update(remark: RemarkEntity)

    @Delete
    suspend fun delete(remark: RemarkEntity)

    @Query("SELECT * FROM remarks WHERE auditId = :auditId")
    suspend fun getByAudit(auditId: Long): List<RemarkEntity>

    @Query("SELECT * FROM remarks WHERE id = :id")
    suspend fun getById(id: Long): RemarkEntity?

    @Query("SELECT * FROM remarks WHERE questionScoreId = :scoreId")
    suspend fun getByQuestionScore(scoreId: Long): RemarkEntity?
}