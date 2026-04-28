package com.sys.audit5s.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.db.entity.QuestionScoreEntity
import com.sys.audit5s.data.db.entity.RemarkEntity
import com.sys.audit5s.data.db.entity.RemarkPhotoEntity
import com.sys.audit5s.data.repository.QuestionScoreRepository
import com.sys.audit5s.data.repository.RemarkRepository
import com.sys.audit5s.util.AuditQuestion
import com.sys.audit5s.util.AuditQuestions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionUiState(
    val question: AuditQuestion,
    val scoreEntity: QuestionScoreEntity?,
    val score: Int = 3
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val questionScoreRepository: QuestionScoreRepository,
    private val remarkRepository: RemarkRepository
) : ViewModel() {

    private val _questions = MutableStateFlow<List<QuestionUiState>>(emptyList())
    val questions: StateFlow<List<QuestionUiState>> = _questions

    private val _totalScore = MutableStateFlow(0)
    val totalScore: StateFlow<Int> = _totalScore

    private var auditId: Long = -1L
    private var category: String = ""

    fun init(auditId: Long, category: String) {
        this.auditId = auditId
        this.category = category
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            val questionList = AuditQuestions.getByCategory(category)
            val savedScores = questionScoreRepository.getByCategory(auditId, category)
            val uiStates = questionList.map { question ->
                val saved = savedScores.find { it.questionNumber == question.number }
                QuestionUiState(
                    question = question,
                    scoreEntity = saved,
                    score = saved?.score ?: 3
                )
            }
            _questions.value = uiStates
            recalcTotal()
        }
    }

    fun saveAllDefaultScores(onDone: () -> Unit) {
        viewModelScope.launch {
            val savedScores = questionScoreRepository.getByCategory(auditId, category)
            _questions.value.forEach { questionState ->
                val alreadySaved = savedScores.any {
                    it.questionNumber == questionState.question.number
                }
                if (!alreadySaved) {
                    questionScoreRepository.insert(
                        QuestionScoreEntity(
                            auditId = auditId,
                            category = category,
                            questionNumber = questionState.question.number,
                            questionText = questionState.question.text,
                            score = 3
                        )
                    )
                }
            }
            onDone()
        }
    }

    fun saveScore(
        questionNumber: Int,
        score: Int,
        remarkDescription: String = "",
        remarkDeadline: String = "",
        remarkExecutor: String = "",
        photoUris: List<String> = emptyList(),
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = _questions.value.find {
                it.question.number == questionNumber
            } ?: return@launch

            val scoreId = if (current.scoreEntity != null) {
                questionScoreRepository.update(current.scoreEntity.copy(score = score))
                current.scoreEntity.id
            } else {
                questionScoreRepository.insert(
                    QuestionScoreEntity(
                        auditId = auditId,
                        category = category,
                        questionNumber = questionNumber,
                        questionText = current.question.text,
                        score = score
                    )
                )
            }

            // Если оценка стала 3 — удаляем замечание
            if (score == 3) {
                val existingRemark = remarkRepository.getByQuestionScore(scoreId)
                if (existingRemark != null) {
                    remarkRepository.delete(existingRemark)
                }
            }

            if (score < 3 && remarkDescription.isNotBlank()) {
                val existing = remarkRepository.getByQuestionScore(scoreId)
                val remarkId = if (existing != null) {
                    remarkRepository.update(
                        existing.copy(
                            description = remarkDescription,
                            deadlineDate = remarkDeadline,
                            executorName = remarkExecutor
                        )
                    )
                    existing.id
                } else {
                    remarkRepository.insert(
                        RemarkEntity(
                            questionScoreId = scoreId,
                            auditId = auditId,
                            deadlineDate = remarkDeadline,
                            executorName = remarkExecutor,
                            description = remarkDescription
                        )
                    )
                }
                photoUris.forEach { uri ->
                    remarkRepository.insertPhoto(
                        RemarkPhotoEntity(
                            remarkId = remarkId,
                            photoUri = uri
                        )
                    )
                }
            }

            loadQuestions()
            onDone()
        }
    }

    suspend fun getRemarkForQuestion(questionNumber: Int): RemarkEntity? {
        val scoreEntity = _questions.value
            .find { it.question.number == questionNumber }
            ?.scoreEntity ?: return null
        return remarkRepository.getByQuestionScore(scoreEntity.id)
    }

    suspend fun getPhotosForRemark(remarkId: Long): List<RemarkPhotoEntity> =
        remarkRepository.getPhotosByRemark(remarkId)

    private fun recalcTotal() {
        _totalScore.value = _questions.value.sumOf { it.score }
    }

    fun getMaxScore(): Int = AuditQuestions.getByCategory(category).size * 3
    fun getPassingScore(): Int = AuditQuestions.passingScore(category)
    fun getCurrentScore(): Int = _questions.value.sumOf { it.score }
    fun getCategory(): String = category
}