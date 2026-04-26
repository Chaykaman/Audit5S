package com.sys.audit5s.util

import android.content.Context
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object ExcelGenerator {

    fun generate(context: Context, data: AuditExportData): File {
        val fileName = "audit_${data.date.replace(".", "_")}_${data.sectionName}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)

        val workbook = XSSFWorkbook()

        val headerStyle = createHeaderStyle(workbook)
        val titleStyle = createTitleStyle(workbook)
        val labelStyle = createLabelStyle(workbook)
        val normalStyle = createNormalStyle(workbook)
        val passedStyle = createPassedStyle(workbook)
        val failedStyle = createFailedStyle(workbook)

        createSummarySheet(
            workbook, data,
            titleStyle, headerStyle, labelStyle,
            normalStyle, passedStyle, failedStyle
        )
        createRemarksSheet(workbook, data, headerStyle, normalStyle)

        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()

        return file
    }

    private fun createSummarySheet(
        workbook: XSSFWorkbook,
        data: AuditExportData,
        titleStyle: XSSFCellStyle,
        headerStyle: XSSFCellStyle,
        labelStyle: XSSFCellStyle,
        normalStyle: XSSFCellStyle,
        passedStyle: XSSFCellStyle,
        failedStyle: XSSFCellStyle
    ) {
        val sheet = workbook.createSheet("Итоги аудита")
        sheet.setColumnWidth(0, 7000)
        sheet.setColumnWidth(1, 10000)
        sheet.setColumnWidth(2, 5000)
        sheet.setColumnWidth(3, 5000)

        var rowNum = 0

        // Заголовок
        val titleRow = sheet.createRow(rowNum++)
        titleRow.heightInPoints = 30f
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("АУДИТ 5С — ${data.sectionName}")
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 4))

        rowNum++ // Пустая строка

        // Заголовок секции
        val infoHeader = sheet.createRow(rowNum++)
        val infoHeaderCell = infoHeader.createCell(0)
        infoHeaderCell.setCellValue("ДАННЫЕ АУДИТА")
        infoHeaderCell.cellStyle = headerStyle
        sheet.addMergedRegion(CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4))

        mapOf(
            "Дата проведения" to data.date,
            "Аудитор" to data.auditorName,
            "Представитель цеха" to data.workshopRepName,
            "Участок" to data.sectionName,
            "Итоговая категория" to data.finalCategory,
            "Всего баллов" to data.totalScore.toString()
        ).forEach { (label, value) ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).apply {
                setCellValue(label)
                cellStyle = labelStyle
            }
            row.createCell(1).apply {
                setCellValue(value)
                cellStyle = normalStyle
            }
        }

        rowNum++ // Пустая строка

        // Категории
        val catHeader = sheet.createRow(rowNum++)
        catHeader.createCell(0).apply {
            setCellValue("РЕЗУЛЬТАТЫ ПО КАТЕГОРИЯМ")
            cellStyle = headerStyle
        }
        sheet.addMergedRegion(CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4))

        val catColHeader = sheet.createRow(rowNum++)
        listOf("Категория", "Баллы", "Максимум", "Статус").forEachIndexed { i, h ->
            catColHeader.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        data.categoryResults.forEach { result ->
            val row = sheet.createRow(rowNum++)
            val style = if (result.isPassed) passedStyle else failedStyle
            row.createCell(0).apply { setCellValue(result.category); cellStyle = style }
            row.createCell(1).apply { setCellValue(result.score.toDouble()); cellStyle = style }
            row.createCell(2).apply { setCellValue(result.maxScore.toDouble()); cellStyle = style }
            row.createCell(3).apply {
                setCellValue(if (result.isPassed) "Пройдена" else "Не пройдена")
                cellStyle = style
            }
        }
    }

    private fun createRemarksSheet(
        workbook: XSSFWorkbook,
        data: AuditExportData,
        headerStyle: XSSFCellStyle,
        normalStyle: XSSFCellStyle
    ) {
        val sheet = workbook.createSheet("Замечания")
        listOf(3000, 3000, 3000, 8000, 8000, 5000, 4000, 4000)
            .forEachIndexed { i, w -> sheet.setColumnWidth(i, w) }

        var rowNum = 0

        val headerRow = sheet.createRow(rowNum++)
        listOf(
            "№", "Категория", "Вопрос №", "Текст вопроса",
            "Мероприятие", "Исполнитель", "Срок", "Выполнено"
        ).forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        data.remarks.forEachIndexed { index, remark ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).apply { setCellValue((index + 1).toDouble()); cellStyle = normalStyle }
            row.createCell(1).apply { setCellValue(remark.category); cellStyle = normalStyle }
            row.createCell(2).apply { setCellValue(remark.questionNumber.toDouble()); cellStyle = normalStyle }
            row.createCell(3).apply { setCellValue(remark.questionText); cellStyle = normalStyle }
            row.createCell(4).apply { setCellValue(remark.description); cellStyle = normalStyle }
            row.createCell(5).apply { setCellValue(remark.executor); cellStyle = normalStyle }
            row.createCell(6).apply { setCellValue(remark.deadline); cellStyle = normalStyle }
            row.createCell(7).apply { setCellValue(remark.actualDate ?: "—"); cellStyle = normalStyle }
        }
    }

    private fun createTitleStyle(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        val font = wb.createFont()
        font.bold = true
        font.fontHeightInPoints = 16
        font.color = IndexedColors.WHITE.index
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.ROYAL_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        return style
    }

    private fun createHeaderStyle(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        val font = wb.createFont()
        font.bold = true
        font.color = IndexedColors.WHITE.index
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.ROYAL_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        return style
    }

    private fun createLabelStyle(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        val font = wb.createFont()
        font.bold = true
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        return style
    }

    private fun createNormalStyle(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        style.wrapText = true
        return style
    }

    private fun createPassedStyle(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        style.fillForegroundColor = IndexedColors.LIGHT_GREEN.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun createFailedStyle(wb: XSSFWorkbook): XSSFCellStyle {
        val style = wb.createCellStyle() as XSSFCellStyle
        style.fillForegroundColor = IndexedColors.ROSE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.setBorderBottom(BorderStyle.THIN)
        style.setBorderTop(BorderStyle.THIN)
        style.setBorderLeft(BorderStyle.THIN)
        style.setBorderRight(BorderStyle.THIN)
        style.alignment = HorizontalAlignment.CENTER
        return style
    }
}