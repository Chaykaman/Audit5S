package com.sys.audit5s.ui.summary

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentSummaryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SummaryFragment : Fragment(R.layout.fragment_summary) {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SummaryViewModel by viewModels()
    private lateinit var remarkAdapter: RemarkAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSummaryBinding.bind(view)

        val auditId = arguments?.getLong("auditId") ?: -1L
        viewModel.load(auditId)

        setupToolbar()
        setupRemarks()
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

    private fun setupRemarks() {
        remarkAdapter = RemarkAdapter { remarkItem ->
            findNavController().navigate(
                R.id.remarkDetailFragment,
                bundleOf("remarkId" to remarkItem.remark.id)
            )
        }
        binding.rvRemarks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRemarks.adapter = remarkAdapter
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
            viewModel.categoryResults.collect { results ->
                buildCategoryChips(results)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.remarks.collect { remarks ->
                remarkAdapter.submitList(remarks)
                binding.cardRemarks.visibility =
                    if (remarks.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun buildCategoryChips(results: List<CategoryResult>) {
        binding.categoriesContainer.removeAllViews()
        results.forEach { result ->
            val chip = TextView(requireContext()).apply {
                text = result.category
                textSize = 12f
                setPadding(16, 8, 16, 8)
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (result.isPassed) R.color.white else R.color.text_secondary
                    )
                )
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (result.isPassed) R.drawable.bg_chip_passed
                    else R.drawable.bg_chip_failed
                )
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(4, 0, 4, 0)
                layoutParams = params
            }
            binding.categoriesContainer.addView(chip)
        }
    }

    private fun setupButtons(auditId: Long) {
        binding.btnNewAudit.setOnClickListener {
            findNavController().navigate(R.id.startAuditFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}