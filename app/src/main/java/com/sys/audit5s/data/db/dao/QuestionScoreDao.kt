package com.sys.audit5s.data.db.dao

import androidx.room.*
import com.sys.audit5s.data.db.entity.QuestionScoreEntity

@Dao
interface QuestionScoreDao {

    @Insert
    suspend fun insert(score: QuestionScoreEntity): Long

    @Insert
    suspend fun insertAll(scores: List<QuestionScoreEntity>)

    @Update
    suspend fun update(score: QuestionScoreEntity)

    @Query("SELECT * FROM question_scores WHERE auditId = :auditId AND category = :category")
    suspend fun getByCategory(auditId: Long, category: String): List<QuestionScoreEntity>

    @Query("SELECT * FROM question_scores WHERE auditId = :auditId")
    suspend fun getByAudit(auditId: Long): List<QuestionScoreEntity>

    @Query("SELECT * FROM question_scores WHERE id = :id")
    suspend fun getById(id: Long): QuestionScoreEntity?

    @Query("SELECT SUM(score) FROM question_scores WHERE auditId = :auditId AND category = :category")
    suspend fun getSumByCategory(auditId: Long, category: String): Int?
}