package com.sys.audit5s.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupVersion()
        setupAuditorName()
        setupObservers()
        viewModel.loadStats()
    }

    private fun setupVersion() {
        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        binding.tvVersion.text = versionName
        binding.tvDeveloper.text = "АО УК «БМЗ»"
    }

    private fun setupAuditorName() {
        val prefs = requireContext()
            .getSharedPreferences("audit5s_prefs", android.content.Context.MODE_PRIVATE)
        val savedName = prefs.getString("auditor_name", "") ?: ""

        updateAuditorDisplay(savedName)

        binding.tvEdit.setOnClickListener {
            val input = EditText(requireContext()).apply {
                setText(savedName)
                hint = "Введите ФИО"
                setPadding(48, 24, 48, 24)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Имя аудитора")
                .setView(input)
                .setPositiveButton("Сохранить") { _, _ ->
                    val name = input.text.toString().trim()
                    prefs.edit().putString("auditor_name", name).apply()
                    updateAuditorDisplay(name)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun updateAuditorDisplay(name: String) {
        if (name.isNotEmpty()) {
            binding.tvAuditorName.text = name
            val initials = name.split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.toString() }
                .joinToString("")
                .uppercase()
            binding.tvAvatar.text = initials
        } else {
            binding.tvAuditorName.text = "Не указано"
            binding.tvAvatar.text = "?"
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stats.collect { stats ->
                binding.tvTotalAudits.text = stats.totalAudits.toString()
                binding.tvAudits5C.text = stats.audits5C.toString()
                binding.tvTotalRemarks.text = stats.totalRemarks.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}