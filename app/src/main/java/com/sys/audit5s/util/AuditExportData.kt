package com.sys.audit5s.util

data class AuditExportData(
    val date: String,
    val auditorName: String,
    val workshopRepName: String,
    val sectionName: String,
    val finalCategory: String,
    val totalScore: Int,
    val categoryResults: List<CategoryExportResult>,
    val remarks: List<RemarkExportItem>
)

data class CategoryExportResult(
    val category: String,
    val score: Int,
    val maxScore: Int,
    val isPassed: Boolean
)

data class RemarkExportItem(
    val category: String,
    val questionNumber: Int,
    val questionText: String,
    val score: Int,
    val description: String,
    val executor: String,
    val deadline: String,
    val actualDate: String?,
    val photos: List<String> = emptyList() // пути к фото
)
