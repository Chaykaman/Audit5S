package com.sys.audit5s.data.repository

import com.sys.audit5s.data.db.dao.QuestionScoreDao
import com.sys.audit5s.data.db.entity.QuestionScoreEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionScoreRepository @Inject constructor(
    private val questionScoreDao: QuestionScoreDao
) {
    suspend fun insert(score: QuestionScoreEntity): Long =
        questionScoreDao.insert(score)

    suspend fun insertAll(scores: List<QuestionScoreEntity>) =
        questionScoreDao.insertAll(scores)

    suspend fun update(score: QuestionScoreEntity) =
        questionScoreDao.update(score)

    suspend fun getById(id: Long): QuestionScoreEntity? =
        questionScoreDao.getById(id)

    suspend fun getByAudit(auditId: Long): List<QuestionScoreEntity> =
        questionScoreDao.getByAudit(auditId)

    suspend fun getByCategory(auditId: Long, category: String): List<QuestionScoreEntity> =
        questionScoreDao.getByCategory(auditId, category)

    suspend fun getSumByCategory(auditId: Long, category: String): Int =
        questionScoreDao.getSumByCategory(auditId, category) ?: 0
}