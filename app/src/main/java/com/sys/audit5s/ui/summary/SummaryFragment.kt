package com.sys.audit5s.ui.summary

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentSummaryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SummaryFragment : Fragment(R.layout.fragment_summary) {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SummaryViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSummaryBinding.bind(view)

        val auditId = arguments?.getLong("auditId") ?: -1L
        viewModel.load(auditId)

        setupToolbar()
        setupObservers()
        setupButtons(auditId)
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
            viewModel.audit.collect { audit ->
                audit ?: return@collect
                binding.tvDate.text = audit.date
                binding.tvAuditor.text = audit.auditorName
                binding.tvWorkshopRep.text = audit.workshopRepName
                binding.tvSection.text = audit.sectionName
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalScore.collect { score ->
                binding.tvTotalScore.text = score.toString()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.finalCategory.collect { category ->
                binding.tvFinalCategory.text = "Категория $category"
                binding.tvMaxScore.text = "итоговый результат"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.remarks.collect { remarks ->
                buildRemarkViews(remarks)
                binding.cardRemarks.visibility =
                    if (remarks.isEmpty()) View.GONE else View.VISIBLE
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            exportViewModel.exportState.collect { state ->
                when (state) {
                    is ExportState.Loading -> {
                        binding.btnExportPdf.isEnabled = false
                        binding.btnExportExcel.isEnabled = false
                    }
                    is ExportState.Success -> {
                        binding.btnExportPdf.isEnabled = true
                        binding.btnExportExcel.isEnabled = true
                        shareFile(state.file)
                    }
                    is ExportState.Error -> {
                        binding.btnExportPdf.isEnabled = true
                        binding.btnExportExcel.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            "Ошибка: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun buildRemarkViews(remarks: List<RemarkUiItem>) {
        binding.remarksContainer.removeAllViews()
        val inflater = layoutInflater

        remarks.forEach { item ->
            val itemView = inflater.inflate(
                R.layout.item_remark,
                binding.remarksContainer,
                false
            )
            itemView.findViewById<TextView>(R.id.tvRemarkCategory).text =
                "Категория ${item.category} · Вопрос ${item.questionNumber}"
            itemView.findViewById<TextView>(R.id.tvRemarkDescription).text =
                item.remark.description
            itemView.findViewById<TextView>(R.id.tvRemarkDeadline).text =
                "Срок: ${item.remark.deadlineDate}"
            itemView.setOnClickListener {
                findNavController().navigate(
                    R.id.remarkDetailFragment,
                    bundleOf("remarkId" to item.remark.id)
                )
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 8)
            itemView.layoutParams = params

            binding.remarksContainer.addView(itemView)
        }
    }



    private fun setupButtons(auditId: Long) {
        binding.btnNewAudit.setOnClickListener {
            findNavController().navigate(R.id.startAuditFragment)
        }

        binding.btnEditAudit.setOnClickListener {
            val categories = arrayOf("1С", "2С", "3С", "4С", "5С")
            AlertDialog.Builder(requireContext())
                .setTitle("Редактирование аудита")
                .setItems(categories) { _, which ->
                    findNavController().navigate(
                        R.id.categoryFragment,
                        bundleOf(
                            "auditId" to auditId,
                            "category" to categories[which]
                        )
                    )
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        binding.btnExportPdf.setOnClickListener {
            exportViewModel.exportPdf(requireContext(), auditId)
        }

        binding.btnExportExcel.setOnClickListener {
            exportViewModel.exportExcel(requireContext(), auditId)
        }
    }

    private fun shareFile(file: java.io.File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val mimeType = when {
            file.name.endsWith(".pdf") -> "application/pdf"
            file.name.endsWith(".xlsx") ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            file.name.endsWith(".zip") -> "application/zip"
            else -> "*/*"
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Поделиться файлом"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}