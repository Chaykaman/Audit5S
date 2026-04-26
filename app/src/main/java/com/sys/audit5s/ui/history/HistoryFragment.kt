package com.sys.audit5s.ui.history

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: AuditAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        binding.toolbar.title = "История аудитов"

        setupRecyclerView()
        setupObservers()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.audits.collect { audits ->
                binding.toolbar.subtitle =
                    if (audits.isEmpty()) "Нет аудитов"
                    else "Проведено аудитов: ${audits.size}"
            }
        }
    }



    private fun setupRecyclerView() {
        adapter = AuditAdapter { audit ->
            findNavController().navigate(
                R.id.summaryFragment,
                bundleOf("auditId" to audit.id)
            )
        }
        binding.rvAudits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAudits.adapter = adapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.audits.collect { audits ->
                adapter.submitList(audits)

                if (audits.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.rvAudits.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.rvAudits.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}