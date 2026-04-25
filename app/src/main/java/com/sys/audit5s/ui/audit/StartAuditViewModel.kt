package com.sys.audit5s.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.db.entity.AuditEntity
import com.sys.audit5s.data.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartAuditViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    var auditId: Long = -1L
        private set

    fun createAudit(
        date: String,
        auditorName: String,
        workshopRepName: String,
        sectionName: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val audit = AuditEntity(
                date = date,
                auditorName = auditorName,
                workshopRepName = workshopRepName,
                sectionName = sectionName
            )
            auditId = auditRepository.insert(audit)
            onSuccess(auditId)
        }
    }
}