package com.sys.audit5s.ui.remark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sys.audit5s.data.db.entity.RemarkEntity
import com.sys.audit5s.data.db.entity.RemarkPhotoEntity
import com.sys.audit5s.data.repository.QuestionScoreRepository
import com.sys.audit5s.data.repository.RemarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemarkDetailUiState(
    val remark: RemarkEntity,
    val category: String,
    val questionNumber: Int,
    val questionText: String,
    val score: Int,
    val photos: List<RemarkPhotoEntity>
)

@HiltViewModel
class RemarkDetailViewModel @Inject constructor(
    private val remarkRepository: RemarkRepository,
    private val questionScoreRepository: QuestionScoreRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RemarkDetailUiState?>(null)
    val state: StateFlow<RemarkDetailUiState?> = _state

    fun load(remarkId: Long) {
        viewModelScope.launch {
            val remark = remarkRepository.getById(remarkId) ?: return@launch
            val score = questionScoreRepository.getById(remark.questionScoreId)
            val photos = remarkRepository.getPhotosByRemark(remarkId)

            _state.value = RemarkDetailUiState(
                remark = remark,
                category = score?.category ?: "—",
                questionNumber = score?.questionNumber ?: 0,
                questionText = score?.questionText ?: "—",
                score = score?.score ?: 0,
                photos = photos
            )
        }
    }

    fun markAsDone(date: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val current = _state.value ?: return@launch
            remarkRepository.update(
                current.remark.copy(actualDate = date)
            )
            load(current.remark.id)
            onDone()
        }
    }

    fun savePhotosToGallery(context: android.content.Context, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val state = _state.value ?: return@launch
            var savedCount = 0

            state.photos.forEach { photo ->
                try {
                    val file = java.io.File(photo.photoUri)
                    if (!file.exists()) return@forEach

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                                "audit_photo_${System.currentTimeMillis()}.jpg")
                            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                                android.os.Environment.DIRECTORY_PICTURES + "/Audit5S")
                        }
                        val uri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                        )
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { out ->
                                file.inputStream().copyTo(out)
                            }
                            savedCount++
                        }
                    } else {
                        val picturesDir = java.io.File(
                            android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_PICTURES
                            ), "Audit5S"
                        ).apply { mkdirs() }
                        val dest = java.io.File(picturesDir, "audit_photo_${System.currentTimeMillis()}.jpg")
                        file.copyTo(dest, overwrite = true)
                        android.media.MediaScannerConnection.scanFile(
                            context, arrayOf(dest.absolutePath), null, null
                        )
                        savedCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onDone(savedCount)
        }
    }
}