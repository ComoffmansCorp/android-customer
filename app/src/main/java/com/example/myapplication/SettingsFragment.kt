package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private val roleLabels = mapOf(
        "SUPER_ADMIN" to "Супер-администратор",
        "TENANT_ADMIN" to "Администратор",
        "DISPATCHER" to "Диспетчер",
        "ELECTRICIAN" to "Инспектор"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentSettingsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle("Настройки")
        (activity as? MainActivity)?.setNotificationIconVisible(false)

        b.tvFullName.text = AuthManager.fullName ?: "—"
        b.tvRole.text = AuthManager.role?.let { roleLabels[it] ?: it } ?: "—"

        b.etBaseUrl.setText(AppSettings.baseUrl)
        b.btnSaveUrl.setOnClickListener {
            AppSettings.baseUrl = b.etBaseUrl.text?.toString().orEmpty()
            b.etBaseUrl.setText(AppSettings.baseUrl)
            Toast.makeText(requireContext(), "Адрес сервера сохранён", Toast.LENGTH_SHORT).show()
        }

        b.btnLogout.setOnClickListener {
            AuthManager.clear()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
