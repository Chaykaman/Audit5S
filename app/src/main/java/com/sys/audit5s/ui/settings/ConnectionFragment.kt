package com.sys.audit5s.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.sys.audit5s.R
import com.sys.audit5s.databinding.Fragment1cConnectionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConnectionFragment : Fragment(R.layout.fragment_1c_connection) {

    private var _binding: Fragment1cConnectionBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireContext().getSharedPreferences("connection_prefs", android.content.Context.MODE_PRIVATE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = Fragment1cConnectionBinding.bind(view)

        setupToolbar()
        loadSavedSettings()
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

    private fun loadSavedSettings() {
        val address = prefs.getString("server_address", "") ?: ""
        val username = prefs.getString("username", "") ?: ""
        val isConnected = prefs.getBoolean("is_connected", false)

        binding.etServerAddress.setText(address)
        binding.etUsername.setText(username)

        updateConnectionStatus(isConnected, address)

        if (isConnected) {
            binding.btnDisconnect.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() {
        binding.btnTestConnection.setOnClickListener {
            val address = binding.etServerAddress.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (address.isEmpty()) {
                binding.etServerAddress.error = "Введите адрес сервера"
                return@setOnClickListener
            }

            // Показываем процесс проверки
            binding.btnTestConnection.isEnabled = false
            binding.btnTestConnection.text = "Проверяем..."

            // Симулируем проверку подключения
            binding.root.postDelayed({
                if (_binding == null) return@postDelayed
                binding.btnTestConnection.isEnabled = true
                binding.btnTestConnection.text = "Проверить подключение"

                // Пока показываем заглушку — реальная проверка будет добавлена позже
                Toast.makeText(
                    requireContext(),
                    "Функция проверки будет добавлена при интеграции с 1С",
                    Toast.LENGTH_LONG
                ).show()
            }, 1500)
        }

        binding.btnSaveConnection.setOnClickListener {
            val address = binding.etServerAddress.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (address.isEmpty()) {
                binding.etServerAddress.error = "Введите адрес сервера"
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                binding.etUsername.error = "Введите имя пользователя"
                return@setOnClickListener
            }

            prefs.edit()
                .putString("server_address", address)
                .putString("username", username)
                .putString("password", password)
                .putBoolean("is_connected", true)
                .apply()

            updateConnectionStatus(true, address)
            binding.btnDisconnect.visibility = View.VISIBLE

            Toast.makeText(
                requireContext(),
                "Настройки сохранены",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnDisconnect.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Отключиться от 1С?")
                .setMessage("Сохранённые настройки подключения будут удалены")
                .setPositiveButton("Да") { _, _ ->
                    prefs.edit()
                        .remove("server_address")
                        .remove("username")
                        .remove("password")
                        .putBoolean("is_connected", false)
                        .apply()

                    binding.etServerAddress.setText("")
                    binding.etUsername.setText("")
                    binding.etPassword.setText("")
                    binding.btnDisconnect.visibility = View.GONE
                    updateConnectionStatus(false, "")

                    Toast.makeText(
                        requireContext(),
                        "Отключено от 1С",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean, address: String) {
        if (isConnected) {
            binding.statusIndicator.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.score_3)
            binding.tvConnectionStatus.text = "Подключено"
            binding.tvConnectionDetails.text = address
        } else {
            binding.statusIndicator.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.error)
            binding.tvConnectionStatus.text = "Не подключено"
            binding.tvConnectionDetails.text = "Настройте подключение к базе 1С"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}