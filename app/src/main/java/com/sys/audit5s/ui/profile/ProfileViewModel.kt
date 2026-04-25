package com.sys.audit5s.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.repository.AuditRepository
import com.sys.audit5s.data.repository.RemarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val totalAudits: Int,
    val audits5C: Int,
    val totalRemarks: Int
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auditRepository: AuditRepository,
    private val remarkRepository: RemarkRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(ProfileStats(0, 0, 0))
    val stats: StateFlow<ProfileStats> = _stats

    fun loadStats() {
        viewModelScope.launch {
            val audits = auditRepository.getAllAudits().first()
            val total = audits.size
            val audits5C = audits.count { it.finalCategory == "5С" }
            val remarks = audits.sumOf { audit ->
                remarkRepository.getByAudit(audit.id).size
            }
            _stats.value = ProfileStats(total, audits5C, remarks)
        }
    }
}