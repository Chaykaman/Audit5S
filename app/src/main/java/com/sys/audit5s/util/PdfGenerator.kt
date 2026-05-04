package com.sys.audit5s.util

import android.content.Context
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generate(context: Context, data: AuditExportData): File {
        val fileName = "audit_${data.date.replace(".", "_")}_${data.sectionName}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)

        val document = Document(PageSize.A4, 36f, 36f, 54f, 36f)
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        val baseFont = BaseFont.createFont(
            "assets/fonts/arial.ttf",
            BaseFont.IDENTITY_H,
            BaseFont.EMBEDDED
        )
        val fontTitle = Font(baseFont, 18f, Font.BOLD, BaseColor(0, 79, 158))
        val fontHeader = Font(baseFont, 13f, Font.BOLD, BaseColor(0, 14, 28))
        val fontNormal = Font(baseFont, 10f, Font.NORMAL, BaseColor(0, 14, 28))
        val fontSmall = Font(baseFont, 9f, Font.NORMAL, BaseColor(163, 168, 165))
        val fontBold = Font(baseFont, 10f, Font.BOLD, BaseColor(0, 14, 28))
        val fontWhite = Font(baseFont, 10f, Font.BOLD, BaseColor.WHITE)

        val title = Paragraph("АУДИТ 5С", fontTitle)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 4f
        document.add(title)

        val subtitle = Paragraph("Результаты проверки", fontSmall)
        subtitle.alignment = Element.ALIGN_CENTER
        subtitle.spacingAfter = 16f
        document.add(subtitle)

        document.add(addLine())

        document.add(sectionTitle("ДАННЫЕ АУДИТА", fontHeader))

        val infoTable = PdfPTable(2)
        infoTable.widthPercentage = 100f
        infoTable.setWidths(floatArrayOf(1f, 2f))
        infoTable.spacingAfter = 12f

        addInfoRow(infoTable, "Дата проведения", data.date, fontSmall, fontNormal)
        addInfoRow(infoTable, "Аудитор", data.auditorName, fontSmall, fontNormal)
        addInfoRow(infoTable, "Представитель цеха", data.workshopRepName, fontSmall, fontNormal)
        addInfoRow(infoTable, "Участок", data.sectionName, fontSmall, fontNormal)
        addInfoRow(infoTable, "Итоговая категория", data.finalCategory, fontSmall, fontBold)
        addInfoRow(infoTable, "Всего баллов", data.totalScore.toString(), fontSmall, fontBold)

        document.add(infoTable)
        document.add(addLine())

        document.add(sectionTitle("РЕЗУЛЬТАТЫ ПО КАТЕГОРИЯМ", fontHeader))

        val catTable = PdfPTable(4)
        catTable.widthPercentage = 100f
        catTable.setWidths(floatArrayOf(1f, 1.5f, 1.5f, 1.5f))
        catTable.spacingAfter = 12f

        listOf("Категория", "Баллы", "Максимум", "Статус").forEach { header ->
            val cell = PdfPCell(Phrase(header, fontWhite))
            cell.backgroundColor = BaseColor(0, 79, 158)
            cell.setPadding(6f)
            cell.horizontalAlignment = Element.ALIGN_CENTER
            catTable.addCell(cell)
        }

        data.categoryResults.forEach { result ->
            val bgColor = if (result.isPassed)
                BaseColor(232, 245, 233) else BaseColor(255, 235, 238)
            val statusText = if (result.isPassed) "✓ Пройдена" else "✗ Не пройдена"
            val statusColor = if (result.isPassed)
                BaseColor(46, 125, 50) else BaseColor(198, 40, 40)

            addCatCell(catTable, result.category, fontBold, bgColor)
            addCatCell(catTable, result.score.toString(), fontNormal, bgColor)
            addCatCell(catTable, result.maxScore.toString(), fontNormal, bgColor)

            val statusCell = PdfPCell(
                Phrase(statusText, Font(baseFont, 9f, Font.BOLD, statusColor))
            )
            statusCell.backgroundColor = bgColor
            statusCell.setPadding(6f)
            statusCell.horizontalAlignment = Element.ALIGN_CENTER
            catTable.addCell(statusCell)
        }

        document.add(catTable)

        if (data.remarks.isNotEmpty()) {
            document.add(addLine())
            document.add(sectionTitle("ЗАМЕЧАНИЯ (${data.remarks.size})", fontHeader))

            data.remarks.forEachIndexed { index, remark ->
                val remarkTable = PdfPTable(1)
                remarkTable.widthPercentage = 100f
                remarkTable.spacingAfter = 8f

                val headerCell = PdfPCell(
                    Phrase(
                        "№${index + 1}  |  Категория ${remark.category}  |  " +
                                "Вопрос ${remark.questionNumber}  |  Оценка: ${remark.score}",
                        fontWhite
                    )
                )
                headerCell.backgroundColor = BaseColor(0, 79, 158)
                headerCell.setPadding(6f)
                remarkTable.addCell(headerCell)

                addRemarkRow(remarkTable, "Вопрос:", remark.questionText, fontSmall, fontNormal)
                addRemarkRow(remarkTable, "Мероприятие:", remark.description, fontSmall, fontNormal)
                addRemarkRow(remarkTable, "Исполнитель:", remark.executor, fontSmall, fontNormal)
                addRemarkRow(remarkTable, "Срок:", remark.deadline, fontSmall, fontNormal)
                addRemarkRow(
                    remarkTable, "Выполнено:",
                    remark.actualDate ?: "—",
                    fontSmall, fontNormal
                )

                if (remark.photos.isNotEmpty()) {
                    val photoCell = PdfPCell()
                    photoCell.border = Rectangle.BOX
                    photoCell.borderColor = BaseColor(224, 224, 224)
                    photoCell.setPadding(5f)

                    // Разбиваем фото на строки по 3
                    remark.photos.chunked(3).forEach { photoChunk ->
                        val photoTable = PdfPTable(photoChunk.size)
                        photoTable.widthPercentage = 100f

                        photoChunk.forEach { photoPath ->
                            try {
                                val bitmap = android.graphics.BitmapFactory
                                    .decodeFile(photoPath)
                                    ?: throw Exception("Не удалось загрузить фото")

                                val maxSize = 800
                                val scaledBitmap =
                                    if (bitmap.width > maxSize || bitmap.height > maxSize) {
                                        val scale = maxSize.toFloat() /
                                                maxOf(bitmap.width, bitmap.height)
                                        android.graphics.Bitmap.createScaledBitmap(
                                            bitmap,
                                            (bitmap.width * scale).toInt(),
                                            (bitmap.height * scale).toInt(),
                                            true
                                        )
                                    } else bitmap

                                val stream = java.io.ByteArrayOutputStream()
                                scaledBitmap.compress(
                                    android.graphics.Bitmap.CompressFormat.JPEG,
                                    70,
                                    stream
                                )

                                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                                bitmap.recycle()

                                val image = Image.getInstance(stream.toByteArray())
                                image.scaleToFit(150f, 150f)
                                val imgCell = PdfPCell(image)
                                imgCell.border = Rectangle.NO_BORDER
                                imgCell.setPadding(3f)
                                imgCell.horizontalAlignment = Element.ALIGN_CENTER
                                photoTable.addCell(imgCell)

                            } catch (e: Exception) {
                                val errorCell =
                                    PdfPCell(Phrase("Фото недоступно", fontSmall))
                                errorCell.border = Rectangle.NO_BORDER
                                photoTable.addCell(errorCell)
                            }
                        }
                        photoCell.addElement(photoTable)
                    }
                    remarkTable.addCell(photoCell)
                }

                document.add(remarkTable)
            }
        }

        document.close()
        return file
    }

    private fun sectionTitle(text: String, font: Font): Paragraph {
        val p = Paragraph(text, font)
        p.spacingBefore = 8f
        p.spacingAfter = 8f
        return p
    }

    private fun addLine(): Paragraph {
        val line = Paragraph(
            "─".repeat(80),
            Font(Font.FontFamily.HELVETICA, 6f, Font.NORMAL, BaseColor.LIGHT_GRAY)
        )
        line.spacingBefore = 4f
        line.spacingAfter = 8f
        return line
    }

    private fun addInfoRow(
        table: PdfPTable,
        label: String,
        value: String,
        labelFont: Font,
        valueFont: Font
    ) {
        val labelCell = PdfPCell(Phrase(label, labelFont))
        labelCell.backgroundColor = BaseColor(245, 247, 250)
        labelCell.setPadding(6f)
        labelCell.border = Rectangle.BOX
        labelCell.borderColor = BaseColor(224, 224, 224)
        table.addCell(labelCell)

        val valueCell = PdfPCell(Phrase(value, valueFont))
        valueCell.setPadding(6f)
        valueCell.border = Rectangle.BOX
        valueCell.borderColor = BaseColor(224, 224, 224)
        table.addCell(valueCell)
    }

    private fun addCatCell(
        table: PdfPTable,
        text: String,
        font: Font,
        bgColor: BaseColor
    ) {
        val cell = PdfPCell(Phrase(text, font))
        cell.backgroundColor = bgColor
        cell.setPadding(6f)
        cell.horizontalAlignment = Element.ALIGN_CENTER
        table.addCell(cell)
    }

    private fun addRemarkRow(
        table: PdfPTable,
        label: String,
        value: String,
        labelFont: Font,
        valueFont: Font
    ) {
        val innerTable = PdfPTable(2)
        innerTable.widthPercentage = 100f
        innerTable.setWidths(floatArrayOf(1f, 3f))

        val labelCell = PdfPCell(Phrase(label, labelFont))
        labelCell.backgroundColor = BaseColor(245, 247, 250)
        labelCell.setPadding(5f)
        labelCell.border = Rectangle.NO_BORDER
        innerTable.addCell(labelCell)

        val valueCell = PdfPCell(Phrase(value, valueFont))
        valueCell.setPadding(5f)
        valueCell.border = Rectangle.NO_BORDER
        innerTable.addCell(valueCell)

        val wrapCell = PdfPCell(innerTable)
        wrapCell.border = Rectangle.BOX
        wrapCell.borderColor = BaseColor(224, 224, 224)
        wrapCell.setPadding(0f)
        table.addCell(wrapCell)
    }
}