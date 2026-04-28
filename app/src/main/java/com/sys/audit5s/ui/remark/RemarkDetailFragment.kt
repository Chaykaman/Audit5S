package com.sys.audit5s.ui.remark

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentRemarkDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class RemarkDetailFragment : Fragment(R.layout.fragment_remark_detail) {

    private var _binding: FragmentRemarkDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RemarkDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRemarkDetailBinding.bind(view)

        val remarkId = arguments?.getLong("remarkId") ?: -1L
        viewModel.load(remarkId)

        setupToolbar()
        setupObservers()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                state ?: return@collect

                binding.tvCategory.text =
                    "Категория ${state.category} · Вопрос ${state.questionNumber}"
                binding.tvQuestionText.text = state.questionText

                val colorRes = when (state.score) {
                    0 -> R.color.score_0
                    1 -> R.color.score_1
                    2 -> R.color.score_2
                    else -> R.color.score_3
                }
                binding.tvScore.text = state.score.toString()
                binding.tvScore.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), colorRes)

                binding.tvDescription.text = state.remark.description
                binding.tvExecutor.text = state.remark.executorName
                binding.tvDeadline.text = state.remark.deadlineDate.ifEmpty { "Не указан" }
                binding.tvActualDate.text = state.remark.actualDate ?: "—"

                if (state.remark.actualDate != null) {
                    binding.btnMarkDone.text = "Выполнено: ${state.remark.actualDate} · Снять"
                    binding.btnMarkDone.isEnabled = true
                    binding.btnMarkDone.alpha = 1.0f
                } else {
                    binding.btnMarkDone.text = "Отметить выполненным"
                    binding.btnMarkDone.isEnabled = true
                    binding.btnMarkDone.alpha = 1.0f
                }

                if (state.photos.isNotEmpty()) {
                    binding.cardPhotos.visibility = View.VISIBLE
                    binding.photoContainer.removeAllViews()
                    val sizePx = (90 * resources.displayMetrics.density).toInt()
                    val deleteBtnSize = (24 * resources.displayMetrics.density).toInt()

                    state.photos.forEach { photo ->
                        val frameLayout = FrameLayout(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                                setMargins(0, 0, 12, 0)
                            }
                        }

                        val imageView = ImageView(requireContext()).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            clipToOutline = true
                            background = ContextCompat.getDrawable(
                                requireContext(), R.drawable.bg_photo_preview
                            )
                            setImageURI(android.net.Uri.parse(photo.photoUri))
                            setOnClickListener { showFullScreenPhoto(photo.photoUri) }
                        }

                        val deleteBtn = ImageView(requireContext()).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                deleteBtnSize, deleteBtnSize
                            ).apply {
                                gravity = Gravity.TOP or Gravity.END
                                setMargins(0, 4, 4, 0)
                            }
                            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                            background = ContextCompat.getDrawable(
                                requireContext(), R.drawable.bg_delete_photo
                            )
                            setOnClickListener {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Удалить фото?")
                                    .setMessage("Фото будет удалено безвозвратно")
                                    .setPositiveButton("Удалить") { _, _ ->
                                        viewModel.deletePhoto(requireContext(), photo) {
                                            Toast.makeText(
                                                requireContext(),
                                                "Фото удалено",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .setNegativeButton("Отмена", null)
                                    .show()
                            }
                        }

                        frameLayout.addView(imageView)
                        frameLayout.addView(deleteBtn)
                        binding.photoContainer.addView(frameLayout)
                    }

                    binding.btnSavePhotos.visibility = View.VISIBLE
                    binding.btnSavePhotos.setOnClickListener {
                        viewModel.savePhotosToGallery(requireContext()) { count ->
                            Toast.makeText(
                                requireContext(),
                                "Сохранено фото в галерею: $count",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    binding.cardPhotos.visibility = View.GONE
                    binding.btnSavePhotos.visibility = View.GONE
                }
            }
        }
    }

    private fun showFullScreenPhoto(path: String) {
        val dialog = Dialog(
            requireContext(),
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageURI(android.net.Uri.parse(path))
            setOnClickListener { dialog.dismiss() }
        }
        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun setupButtons() {
        binding.btnMarkDone.setOnClickListener {
            val state = viewModel.state.value

            if (state?.remark?.actualDate != null) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Снять отметку выполнения?")
                    .setMessage("Замечание снова станет невыполненным")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.markAsUndone {
                            Toast.makeText(
                                requireContext(),
                                "Отметка выполнения снята",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            } else {
                val cal = Calendar.getInstance()
                DatePickerDialog(
                    requireContext(),
                    { _, y, m, d ->
                        val date = "%02d.%02d.%d".format(d, m + 1, y)
                        viewModel.markAsDone(date) {
                            Toast.makeText(
                                requireContext(),
                                "Замечание отмечено выполненным",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}