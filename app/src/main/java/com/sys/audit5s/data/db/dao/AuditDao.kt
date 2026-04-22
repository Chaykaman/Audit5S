package com.sys.audit5s.data.db.dao

import androidx.room.*
import com.sys.audit5s.data.db.entity.AuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {

    @Insert
    suspend fun insert(audit: AuditEntity): Long

    @Update
    suspend fun update(audit: AuditEntity)

    @Delete
    suspend fun delete(audit: AuditEntity)

    @Query("SELECT * FROM audits ORDER BY id DESC")
    fun getAllAudits(): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audits WHERE id = :id")
    suspend fun getById(id: Long): AuditEntity?
}