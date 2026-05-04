package com.sys.audit5s.util

import android.content.Context
import android.graphics.BitmapFactory
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object WordGenerator {

    fun generate(context: Context, data: AuditExportData): File {
        val fileName = "audit_${data.date.replace(".", "_")}_${data.sectionName}.docx"
        val file = File(context.getExternalFilesDir(null), fileName)

        val document = XWPFDocument()

        addTitle(document, "АУДИТ 5С")
        addSubtitle(document, "Результаты проверки")
        addSeparator(document)

        addSectionHeader(document, "ДАННЫЕ АУДИТА")
        addInfoTable(document, data)
        addSeparator(document)

        addSectionHeader(document, "РЕЗУЛЬТАТЫ ПО КАТЕГОРИЯМ")
        addCategoryTable(document, data)

        if (data.remarks.isNotEmpty()) {
            addSeparator(document)
            addSectionHeader(document, "ЗАМЕЧАНИЯ (${data.remarks.size})")
            data.remarks.forEachIndexed { index, remark ->
                addRemark(document, index + 1, remark)
            }
        }

        FileOutputStream(file).use { document.write(it) }
        document.close()

        return file
    }

    private fun addTitle(document: XWPFDocument, text: String) {
        val para = document.createParagraph()
        para.alignment = ParagraphAlignment.CENTER
        para.spacingAfter = 100
        val run = para.createRun()
        run.setText(text)
        run.isBold = true
        run.fontSize = 20
        run.color = "004f9e"
    }

    private fun addSubtitle(document: XWPFDocument, text: String) {
        val para = document.createParagraph()
        para.alignment = ParagraphAlignment.CENTER
        para.spacingAfter = 200
        val run = para.createRun()
        run.setText(text)
        run.fontSize = 11
        run.color = "9E9E9E"
    }

    private fun addSeparator(document: XWPFDocument) {
        val para = document.createParagraph()
        para.spacingAfter = 100
        para.spacingBefore = 100
        val pBorder = para.ctp.addNewPPr().addNewPBdr()
        val bottom = pBorder.addNewBottom()
        bottom.`val` = org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        bottom.sz = java.math.BigInteger.valueOf(4)
        bottom.color = "CCCCCC"
    }

    private fun addSectionHeader(document: XWPFDocument, text: String) {
        val para = document.createParagraph()
        para.spacingBefore = 200
        para.spacingAfter = 100
        val run = para.createRun()
        run.setText(text)
        run.isBold = true
        run.fontSize = 13
        run.color = "000E1C"
    }

    private fun addInfoTable(document: XWPFDocument, data: AuditExportData) {
        val table = document.createTable(6, 2)
        table.setWidth("100%")

        val rows = listOf(
            "Дата проведения" to data.date,
            "Аудитор" to data.auditorName,
            "Представитель цеха" to data.workshopRepName,
            "Участок" to data.sectionName,
            "Итоговая категория" to data.finalCategory,
            "Всего баллов" to data.totalScore.toString()
        )

        rows.forEachIndexed { index, (label, value) ->
            val row = table.getRow(index)

            val labelCell = row.getCell(0)
            labelCell.setColor("F5F7FA")
            val labelPara = labelCell.paragraphs[0]
            val labelRun = labelPara.createRun()
            labelRun.setText(label)
            labelRun.fontSize = 10
            labelRun.color = "9E9E9E"

            val valueCell = row.getCell(1)
            val valuePara = valueCell.paragraphs[0]
            val valueRun = valuePara.createRun()
            valueRun.setText(value)
            valueRun.fontSize = 10
            if (index >= 4) valueRun.isBold = true
        }
    }

    private fun addCategoryTable(document: XWPFDocument, data: AuditExportData) {
        val table = document.createTable(data.categoryResults.size + 1, 4)
        table.setWidth("100%")

        // Заголовки
        val headerRow = table.getRow(0)
        listOf("Категория", "Баллы", "Максимум", "Статус").forEachIndexed { i, text ->
            val cell = headerRow.getCell(i)
            cell.setColor("004f9e")
            val run = cell.paragraphs[0].createRun()
            run.setText(text)
            run.isBold = true
            run.color = "FFFFFF"
            run.fontSize = 10
            cell.paragraphs[0].alignment = ParagraphAlignment.CENTER
        }

        // Данные
        data.categoryResults.forEachIndexed { index, result ->
            val row = table.getRow(index + 1)
            val bgColor = if (result.isPassed) "E8F5E9" else "FFEBEE"
            val statusText = if (result.isPassed) "Пройдена" else "Не пройдена"
            val statusColor = if (result.isPassed) "2E7D32" else "C62828"

            listOf(result.category, result.score.toString(),
                result.maxScore.toString(), statusText
            ).forEachIndexed { i, text ->
                val cell = row.getCell(i)
                cell.setColor(bgColor)
                val run = cell.paragraphs[0].createRun()
                run.setText(text)
                run.fontSize = 10
                if (i == 3) run.color = statusColor
                cell.paragraphs[0].alignment = ParagraphAlignment.CENTER
            }
        }
    }

    private fun addRemark(document: XWPFDocument, number: Int, remark: RemarkExportItem) {
        // Заголовок замечания
        val headerPara = document.createParagraph()
        headerPara.spacingBefore = 100

        val headerTable = document.createTable(1, 1)
        headerTable.setWidth("100%")
        val headerCell = headerTable.getRow(0).getCell(0)
        headerCell.setColor("004f9e")
        val headerRun = headerCell.paragraphs[0].createRun()
        headerRun.setText(
            "№$number  |  Категория ${remark.category}  |  " +
                    "Вопрос ${remark.questionNumber}  |  Оценка: ${remark.score}"
        )
        headerRun.isBold = true
        headerRun.color = "FFFFFF"
        headerRun.fontSize = 10

        // Детали замечания
        val detailTable = document.createTable(5, 2)
        detailTable.setWidth("100%")

        listOf(
            "Вопрос:" to remark.questionText,
            "Мероприятие:" to remark.description,
            "Исполнитель:" to remark.executor,
            "Срок:" to remark.deadline,
            "Выполнено:" to (remark.actualDate ?: "—")
        ).forEachIndexed { index, (label, value) ->
            val row = detailTable.getRow(index)

            val labelCell = row.getCell(0)
            labelCell.setColor("F5F7FA")
            labelCell.paragraphs[0].createRun().apply {
                setText(label)
                fontSize = 9
                color = "9E9E9E"
            }

            row.getCell(1).paragraphs[0].createRun().apply {
                setText(value)
                fontSize = 10
            }
        }

        // Фото замечания
        if (remark.photos.isNotEmpty()) {
            val photoPara = document.createParagraph()
            photoPara.spacingBefore = 100

            remark.photos.forEach { photoPath ->
                try {
                    val bitmap = BitmapFactory.decodeFile(photoPath) ?: return@forEach

                    val maxSize = 800
                    val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                        val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else bitmap

                    val stream = ByteArrayOutputStream()
                    scaledBitmap.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG, 70, stream
                    )
                    if (scaledBitmap != bitmap) scaledBitmap.recycle()
                    bitmap.recycle()

                    val imgPara = document.createParagraph()
                    val imgRun = imgPara.createRun()
                    imgRun.addPicture(
                        ByteArrayInputStream(stream.toByteArray()),
                        XWPFDocument.PICTURE_TYPE_JPEG,
                        "photo.jpg",
                        Units.toEMU(150.0),
                        Units.toEMU(150.0)
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Пустая строка после замечания
        document.createParagraph()
    }
}