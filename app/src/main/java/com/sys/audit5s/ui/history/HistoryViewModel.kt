package com.sys.audit5s.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.db.entity.AuditEntity
import com.sys.audit5s.data.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val auditRepository: AuditRepository
) : ViewModel() {

    val audits: StateFlow<List<AuditEntity>> = auditRepository.getAllAudits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteAudit(audit: AuditEntity) {
        viewModelScope.launch {
            auditRepository.delete(audit)
        }
    }
}