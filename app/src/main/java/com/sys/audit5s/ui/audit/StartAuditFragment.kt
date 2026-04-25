package com.sys.audit5s.ui.audit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentStartAuditBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class StartAuditFragment : Fragment(R.layout.fragment_start_audit) {

    private var _binding: FragmentStartAuditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StartAuditViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStartAuditBinding.bind(view)

        setupDatePicker()
        setupStartButton()
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val date = "%02d.%02d.%d".format(day, month + 1, year)
                    binding.etDate.setText(date)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupStartButton() {
        binding.btnStartAudit.setOnClickListener {
            val date = binding.etDate.text.toString().trim()
            val auditor = binding.etAuditor.text.toString().trim()
            val workshopRep = binding.etWorkshopRep.text.toString().trim()
            val section = binding.etSection.text.toString().trim()

            if (!validateFields(date, auditor, workshopRep, section)) return@setOnClickListener

            viewModel.createAudit(date, auditor, workshopRep, section) { auditId ->
                val bundle = Bundle().apply {
                    putLong("auditId", auditId)
                    putString("category", "1С")
                }
                findNavController().navigate(R.id.categoryFragment, bundle)
            }
        }
    }

    private fun validateFields(
        date: String,
        auditor: String,
        workshopRep: String,
        section: String
    ): Boolean {
        var isValid = true

        if (date.isEmpty()) {
            binding.tilDate.error = "Укажите дату"
            isValid = false
        } else {
            binding.tilDate.error = null
        }

        if (auditor.isEmpty()) {
            binding.tilAuditor.error = "Введите ФИО аудитора"
            isValid = false
        } else {
            binding.tilAuditor.error = null
        }

        if (workshopRep.isEmpty()) {
            binding.tilWorkshopRep.error = "Введите ФИО представителя цеха"
            isValid = false
        } else {
            binding.tilWorkshopRep.error = null
        }

        if (section.isEmpty()) {
            binding.tilSection.error = "Введите наименование участка"
            isValid = false
        } else {
            binding.tilSection.error = null
        }

        if (!isValid) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}