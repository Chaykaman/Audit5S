package com.sys.audit5s.ui.history

import androidx.lifecycle.ViewModel
import com.sys.audit5s.data.db.entity.AuditEntity
import com.sys.audit5s.data.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    val audits: Flow<List<AuditEntity>> = auditRepository.getAllAudits()
}