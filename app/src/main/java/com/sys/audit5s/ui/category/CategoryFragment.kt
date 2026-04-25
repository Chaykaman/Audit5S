package com.sys.audit5s.ui.category

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentCategoryBinding
import com.sys.audit5s.databinding.SheetQuestionDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class CategoryFragment : Fragment(R.layout.fragment_category) {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var adapter: QuestionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoryBinding.bind(view)

        val auditId = arguments?.getLong("auditId") ?: -1L
        val category = arguments?.getString("category") ?: "1С"

        viewModel.init(auditId, category)

        setupToolbar(category)
        setupRecyclerView()
        setupObservers(category)
        setupFinishButton(auditId, category)
    }

    private fun setupToolbar(category: String) {
        binding.toolbar.title = "Категория $category"
        binding.toolbar.navigationIcon?.setTint(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white)
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = QuestionAdapter { questionState ->
            showQuestionDetail(questionState)
        }
        binding.rvQuestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuestions.adapter = adapter
    }

    private fun setupObservers(category: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.questions.collect { questions ->
                adapter.submitList(questions.toList())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalScore.collect { score ->
                val max = viewModel.getMaxScore()
                val passing = viewModel.getPassingScore()
                binding.tvTotalScore.text = "$score / $max"
                binding.tvPassingScore.text = "Проходной балл: $passing"
            }
        }
    }

    private fun showQuestionDetail(questionState: QuestionUiState) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = SheetQuestionDetailBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        var selectedScore = questionState.score
        sheetBinding.tvQuestionText.text = questionState.question.text
        updateScoreButtons(sheetBinding, selectedScore)

        listOf(
            sheetBinding.btn0 to 0,
            sheetBinding.btn1 to 1,
            sheetBinding.btn2 to 2,
            sheetBinding.btn3 to 3
        ).forEach { (btn, score) ->
            btn.setOnClickListener {
                selectedScore = score
                updateScoreButtons(sheetBinding, score)
                sheetBinding.remarkGroup.visibility =
                    if (score < 3) View.VISIBLE else View.GONE
            }
        }

        sheetBinding.remarkGroup.visibility =
            if (selectedScore < 3) View.VISIBLE else View.GONE

        sheetBinding.etDeadline.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    sheetBinding.etDeadline.setText("%02d.%02d.%d".format(d, m + 1, y))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        sheetBinding.btnSave.setOnClickListener {
            if (selectedScore < 3) {
                val desc = sheetBinding.etDescription.text.toString().trim()
                val deadline = sheetBinding.etDeadline.text.toString().trim()
                val executor = sheetBinding.etExecutor.text.toString().trim()
                if (desc.isEmpty() || deadline.isEmpty() || executor.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Заполните все поля замечания",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                viewModel.saveScore(
                    questionState.question.number,
                    selectedScore, desc, deadline, executor
                ) { dialog.dismiss() }
            } else {
                viewModel.saveScore(
                    questionState.question.number,
                    selectedScore
                ) { dialog.dismiss() }
            }
        }

        dialog.show()
    }

    private fun updateScoreButtons(
        binding: SheetQuestionDetailBinding,
        selected: Int
    ) {
        listOf(binding.btn0, binding.btn1, binding.btn2, binding.btn3)
            .forEachIndexed { index, btn ->
                btn.alpha = if (index == selected) 1.0f else 0.45f
                btn.scaleX = if (index == selected) 1.05f else 1.0f
                btn.scaleY = if (index == selected) 1.05f else 1.0f
            }
    }

    private fun setupFinishButton(auditId: Long, category: String) {
        binding.btnFinishCategory.setOnClickListener {
            val score = viewModel.getCurrentScore()
            val passing = viewModel.getPassingScore()
            val categories = listOf("1С", "2С", "3С", "4С", "5С")
            val currentIndex = categories.indexOf(category)

            if (score < passing) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Недостаточно баллов")
                    .setMessage(
                        "Набрано $score баллов из $passing необходимых.\n\n" +
                                "Вы можете вернуться и пересмотреть оценки, " +
                                "или завершить аудит с текущим результатом."
                    )
                    .setPositiveButton("Завершить аудит") { _, _ ->
                        viewModel.saveAllDefaultScores {
                            findNavController().navigate(
                                R.id.summaryFragment,
                                bundleOf("auditId" to auditId)
                            )
                        }
                    }
                    .setNegativeButton("Вернуться к вопросам") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }

            viewModel.saveAllDefaultScores {
                val nextIndex = currentIndex + 1
                if (nextIndex < categories.size) {
                    val bundle = bundleOf(
                        "auditId" to auditId,
                        "category" to categories[nextIndex]
                    )
                    findNavController().navigate(R.id.categoryFragment, bundle)
                } else {
                    findNavController().navigate(
                        R.id.summaryFragment,
                        bundleOf("auditId" to auditId)
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}