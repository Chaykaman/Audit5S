package com.sys.audit5s.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.db.entity.AuditEntity
import com.sys.audit5s.data.db.entity.RemarkEntity
import com.sys.audit5s.data.repository.AuditRepository
import com.sys.audit5s.data.repository.QuestionScoreRepository
import com.sys.audit5s.data.repository.RemarkRepository
import com.sys.audit5s.util.AuditQuestions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryResult(
    val category: String,
    val score: Int,
    val maxScore: Int,
    val isPassed: Boolean
)

data class RemarkUiItem(
    val remark: RemarkEntity,
    val category: String,
    val questionNumber: Int
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val auditRepository: AuditRepository,
    private val questionScoreRepository: QuestionScoreRepository,
    private val remarkRepository: RemarkRepository
) : ViewModel() {

    private val _audit = MutableStateFlow<AuditEntity?>(null)
    val audit: StateFlow<AuditEntity?> = _audit

    private val _categoryResults = MutableStateFlow<List<CategoryResult>>(emptyList())
    val categoryResults: StateFlow<List<CategoryResult>> = _categoryResults

    private val _remarks = MutableStateFlow<List<RemarkUiItem>>(emptyList())
    val remarks: StateFlow<List<RemarkUiItem>> = _remarks

    private val _totalScore = MutableStateFlow(0)
    val totalScore: StateFlow<Int> = _totalScore

    private val _finalCategory = MutableStateFlow("")
    val finalCategory: StateFlow<String> = _finalCategory

    fun load(auditId: Long) {
        viewModelScope.launch {
            val audit = auditRepository.getById(auditId) ?: return@launch
            _audit.value = audit

            val allScores = questionScoreRepository.getByAudit(auditId)
            val allRemarks = remarkRepository.getByAudit(auditId)

            // Результаты по категориям
            val categories = AuditQuestions.categories
            val results = categories.map { category ->
                val categoryScores = allScores.filter { it.category == category }
                val score = categoryScores.sumOf { it.score }
                val maxScore = AuditQuestions.getByCategory(category).size * 3
                val passing = AuditQuestions.passingScore(category)

                // Если вопросов не было — значит до этой категории не дошли
                val isPassed = categoryScores.isNotEmpty() && score >= passing
                CategoryResult(category, score, maxScore, isPassed)
            }
            _categoryResults.value = results

            // Итоговый балл
            _totalScore.value = allScores.sumOf { it.score }

            // Финальная категория — последняя пройденная
            val finalCat = results.filter { it.isPassed }.lastOrNull()?.category ?: "—"
            _finalCategory.value = finalCat

            // Сохраняем финальную категорию в БД
            auditRepository.update(audit.copy(finalCategory = finalCat))

            // Замечания с привязкой к вопросам
            val remarkItems = allRemarks.mapNotNull { remark ->
                val score = allScores.find { it.id == remark.questionScoreId }
                if (score != null) {
                    RemarkUiItem(remark, score.category, score.questionNumber)
                } else null
            }
            _remarks.value = remarkItems
        }
    }
}