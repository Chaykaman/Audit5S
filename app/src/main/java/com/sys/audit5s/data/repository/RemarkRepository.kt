package com.sys.audit5s.data.repository

import com.sys.audit5s.data.db.dao.RemarkDao
import com.sys.audit5s.data.db.dao.RemarkPhotoDao
import com.sys.audit5s.data.db.entity.RemarkEntity
import com.sys.audit5s.data.db.entity.RemarkPhotoEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemarkRepository @Inject constructor(
    private val remarkDao: RemarkDao,
    private val remarkPhotoDao: RemarkPhotoDao
) {
    suspend fun insert(remark: RemarkEntity): Long =
        remarkDao.insert(remark)

    suspend fun update(remark: RemarkEntity) =
        remarkDao.update(remark)

    suspend fun delete(remark: RemarkEntity) =
        remarkDao.delete(remark)

    suspend fun getById(id: Long): RemarkEntity? =
        remarkDao.getById(id)

    suspend fun getByAudit(auditId: Long): List<RemarkEntity> =
        remarkDao.getByAudit(auditId)

    suspend fun getByQuestionScore(scoreId: Long): RemarkEntity? =
        remarkDao.getByQuestionScore(scoreId)

    suspend fun insertPhoto(photo: RemarkPhotoEntity): Long =
        remarkPhotoDao.insert(photo)

    suspend fun deletePhoto(photo: RemarkPhotoEntity) =
        remarkPhotoDao.delete(photo)

    suspend fun getPhotosByRemark(remarkId: Long): List<RemarkPhotoEntity> =
        remarkPhotoDao.getByRemark(remarkId)

    suspend fun deleteAllPhotos(remarkId: Long) =
        remarkPhotoDao.deleteAllByRemark(remarkId)
}