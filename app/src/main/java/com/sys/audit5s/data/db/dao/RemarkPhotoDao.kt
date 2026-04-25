package com.sys.audit5s.data.db.dao

import androidx.room.*
import com.sys.audit5s.data.db.entity.RemarkPhotoEntity

@Dao
interface RemarkPhotoDao {

    @Insert
    suspend fun insert(photo: RemarkPhotoEntity): Long

    @Delete
    suspend fun delete(photo: RemarkPhotoEntity)

    @Query("SELECT * FROM remark_photos WHERE remarkId = :remarkId")
    suspend fun getByRemark(remarkId: Long): List<RemarkPhotoEntity>

    @Query("DELETE FROM remark_photos WHERE remarkId = :remarkId")
    suspend fun deleteAllByRemark(remarkId: Long)
}