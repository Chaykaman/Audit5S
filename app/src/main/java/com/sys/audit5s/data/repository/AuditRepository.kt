package com.sys.audit5s.data.repository

import com.sys.audit5s.data.db.dao.AuditDao
import com.sys.audit5s.data.db.entity.AuditEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepository @Inject constructor(
    private val auditDao: AuditDao
) {
    fun getAllAudits(): Flow<List<AuditEntity>> =
        auditDao.getAllAudits()

    suspend fun getById(id: Long): AuditEntity? =
        auditDao.getById(id)

    suspend fun insert(audit: AuditEntity): Long =
        auditDao.insert(audit)

    suspend fun update(audit: AuditEntity) =
        auditDao.update(audit)

    suspend fun delete(audit: AuditEntity) =
        auditDao.delete(audit)
}