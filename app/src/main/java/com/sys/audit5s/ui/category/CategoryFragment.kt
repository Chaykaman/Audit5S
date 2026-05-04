package com.sys.audit5s.ui.category

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
import com.sys.audit5s.util.PhotoManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

@AndroidEntryPoint
class CategoryFragment : Fragment(R.layout.fragment_category) {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var adapter: QuestionAdapter

    private var currentPhotoFile: File? = null
    private var currentPhotoUris = mutableListOf<String>()
    private var currentSheetBinding: SheetQuestionDetailBinding? = null

    private val requestCameraPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "Нет доступа к камере", Toast.LENGTH_SHORT).show()
    }

    private val takePictureLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoFile?.let { file ->
                currentPhotoUris.add(file.absolutePath)
                updatePhotoPreview(currentSheetBinding)
            }
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult

        viewLifecycleOwner.lifecycleScope.launch {
            uris.forEach { uri ->
                val path = PhotoManager.copyPhotoToAppStorage(requireContext(), uri)
                if (path != null) {
                    currentPhotoUris.add(path)
                }
            }
            updatePhotoPreview(currentSheetBinding)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCategoryBinding.bind(view)

        val auditId = arguments?.getLong("auditId") ?: -1L
        val category = arguments?.getString("category") ?: "1С"

        viewModel.init(auditId, category)
        setupToolbar(category)
        setupRecyclerView()
        setupObservers()
        setupFinishButton(auditId, category)
    }

    private fun setupToolbar(category: String) {
        binding.toolbar.title = "Категория $category"
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.white)
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

    private fun setupObservers() {
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

        currentPhotoUris = mutableListOf()
        currentSheetBinding = sheetBinding
        updatePhotoPreview(sheetBinding)

        // Загружаем существующее замечание
        viewLifecycleOwner.lifecycleScope.launch {
            val existing = viewModel.getRemarkForQuestion(questionState.question.number)
            if (existing != null) {
                sheetBinding.etDeadline.setText(existing.deadlineDate)
                sheetBinding.etExecutor.setText(existing.executorName)
                sheetBinding.etDescription.setText(existing.description)
                sheetBinding.remarkGroup.visibility = View.VISIBLE
                val photos = viewModel.getPhotosForRemark(existing.id)
                currentPhotoUris = photos.map { it.photoUri }.toMutableList()
                updatePhotoPreview(sheetBinding)
            }
        }

        sheetBinding.btnAddPhoto.setOnClickListener {
            showPhotoOptions(sheetBinding)
        }

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
                if (desc.isEmpty() || executor.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Заполните все поля замечания",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                viewModel.saveScore(
                    questionState.question.number,
                    selectedScore, desc, deadline, executor,
                    currentPhotoUris.toList()
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

    private fun launchCamera() {
        val file = PhotoManager.createImageFile(requireContext())
        currentPhotoFile = file
        val uri = PhotoManager.getUriForFile(requireContext(), file)
        takePictureLauncher.launch(uri)
    }

    private fun showPhotoOptions(sheetBinding: SheetQuestionDetailBinding) {
        currentSheetBinding = sheetBinding
        AlertDialog.Builder(requireContext())
            .setTitle("Добавить фото")
            .setItems(arrayOf("Сделать фото", "Выбрать из галереи")) { _, which ->
                when (which) {
                    0 -> {
                        val permission = android.Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            launchCamera()
                        } else {
                            requestCameraPermission.launch(permission)
                        }
                    }
                    1 -> pickPhotoLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun updatePhotoPreview(sheetBinding: SheetQuestionDetailBinding?) {
        sheetBinding ?: return
        val container = sheetBinding.photoContainer
        if (container.childCount > 1) {
            container.removeViews(1, container.childCount - 1)
        }
        val sizePx = (90 * resources.displayMetrics.density).toInt()
        currentPhotoUris.forEach { path ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(0, 0, 12, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = ContextCompat.getDrawable(
                    requireContext(), R.drawable.bg_photo_preview
                )
                setImageURI(android.net.Uri.parse(path))
                setOnClickListener { showFullScreenPhoto(path) }
            }
            container.addView(imageView)
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

    private fun updateScoreButtons(binding: SheetQuestionDetailBinding, selected: Int) {
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
                AlertDialog.Builder(requireContext())
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
                    .setNegativeButton("Вернуться к вопросам") { d, _ -> d.dismiss() }
                    .show()
                return@setOnClickListener
            }

            viewModel.saveAllDefaultScores {
                val nextIndex = currentIndex + 1
                if (nextIndex < categories.size) {
                    findNavController().navigate(
                        R.id.categoryFragment,
                        bundleOf(
                            "auditId" to auditId,
                            "category" to categories[nextIndex]
                        )
                    )
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