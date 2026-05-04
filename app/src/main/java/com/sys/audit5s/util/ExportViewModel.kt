package com.sys.audit5s.ui.summary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.repository.AuditRepository
import com.sys.audit5s.data.repository.QuestionScoreRepository
import com.sys.audit5s.data.repository.RemarkRepository
import com.sys.audit5s.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val auditRepository: AuditRepository,
    private val questionScoreRepository: QuestionScoreRepository,
    private val remarkRepository: RemarkRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun exportPdf(context: Context, auditId: Long) {
        export(context, auditId, isPdf = true)
    }

    fun exportExcel(context: Context, auditId: Long) {
        export(context, auditId, isPdf = false)
    }

    fun exportWord(context: Context, auditId: Long) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val data = buildExportData(auditId)
                val file = WordGenerator.generate(context, data)
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Ошибка экспорта")
            }
        }
    }

    private fun export(context: Context, auditId: Long, isPdf: Boolean) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val data = buildExportData(auditId)
                val file = if (isPdf) {
                    PdfGenerator.generate(context, data)
                } else {
                    ExcelGenerator.generate(context, data)
                }
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Ошибка экспорта")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    private suspend fun buildExportData(auditId: Long): AuditExportData {
        val audit = auditRepository.getById(auditId)!!
        val allScores = questionScoreRepository.getByAudit(auditId)
        val allRemarks = remarkRepository.getByAudit(auditId)

        val categories = AuditQuestions.categories
        val categoryResults = categories.map { category ->
            val scores = allScores.filter { it.category == category }
            val score = scores.sumOf { it.score }
            val maxScore = AuditQuestions.getByCategory(category).size * 3
            val passing = AuditQuestions.passingScore(category)
            CategoryExportResult(
                category = category,
                score = score,
                maxScore = maxScore,
                isPassed = scores.isNotEmpty() && score >= passing
            )
        }

        val remarks = allRemarks.mapNotNull { remark ->
            val score = allScores.find { it.id == remark.questionScoreId }
            if (score != null) {
                val photos = remarkRepository.getPhotosByRemark(remark.id)
                RemarkExportItem(
                    category = score.category,
                    questionNumber = score.questionNumber,
                    questionText = score.questionText,
                    score = score.score,
                    description = remark.description,
                    executor = remark.executorName,
                    deadline = remark.deadlineDate,
                    actualDate = remark.actualDate,
                    photos = photos.map { it.photoUri }
                )
            } else null
        }

        return AuditExportData(
            date = audit.date,
            auditorName = audit.auditorName,
            workshopRepName = audit.workshopRepName,
            sectionName = audit.sectionName,
            finalCategory = audit.finalCategory ?: "—",
            totalScore = allScores.sumOf { it.score },
            categoryResults = categoryResults,
            remarks = remarks
        )
    }
}