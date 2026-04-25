package com.sys.audit5s.ui.remark

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
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

                // Вопрос
                binding.tvCategory.text =
                    "Категория ${state.category} · Вопрос ${state.questionNumber}"
                binding.tvQuestionText.text = state.questionText

                // Цвет оценки
                val colorRes = when (state.score) {
                    0 -> R.color.score_0
                    1 -> R.color.score_1
                    2 -> R.color.score_2
                    else -> R.color.score_3
                }
                binding.tvScore.text = state.score.toString()
                binding.tvScore.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), colorRes)

                // Детали замечания
                binding.tvDescription.text = state.remark.description
                binding.tvExecutor.text = state.remark.executorName
                binding.tvDeadline.text = state.remark.deadlineDate
                binding.tvActualDate.text = state.remark.actualDate ?: "—"

                // Кнопка выполнено
                if (state.remark.actualDate != null) {
                    binding.btnMarkDone.text = "Выполнено: ${state.remark.actualDate}"
                    binding.btnMarkDone.isEnabled = false
                    binding.btnMarkDone.alpha = 0.6f
                } else {
                    binding.btnMarkDone.text = "Отметить выполненным"
                    binding.btnMarkDone.isEnabled = true
                    binding.btnMarkDone.alpha = 1.0f
                }

                // Фото
                if (state.photos.isNotEmpty()) {
                    binding.cardPhotos.visibility = View.VISIBLE
                    binding.photoContainer.removeAllViews()
                    state.photos.forEach { photo ->
                        val imageView = ImageView(requireContext()).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                120, 120
                            ).apply { setMargins(0, 0, 8, 0) }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageURI(android.net.Uri.parse(photo.photoUri))
                        }
                        binding.photoContainer.addView(imageView)
                    }
                } else {
                    binding.cardPhotos.visibility = View.GONE
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnMarkDone.setOnClickListener {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}