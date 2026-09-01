package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.FragmentMasterProfileBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.ServiceResponse
import com.example.myapplication.network.UpdateMasterProfileRequest
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class MasterProfileFragment : Fragment() {

    private var _b: FragmentMasterProfileBinding? = null
    private val b get() = _b!!
    private val checkboxes = mutableListOf<CheckBox>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentMasterProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.master_profile))
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        b.btnSave.setOnClickListener { save() }

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val categoriesResult = runCatching { ApiClient.api.getCategories() }
            val servicesResult = runCatching { ApiClient.api.getServices() }
            val profileResult = runCatching { ApiClient.api.getMasterProfile() }

            if (categoriesResult.isFailure || servicesResult.isFailure || profileResult.isFailure) {
                val err = categoriesResult.exceptionOrNull() ?: servicesResult.exceptionOrNull() ?: profileResult.exceptionOrNull()
                Toast.makeText(requireContext(), "Ошибка загрузки: ${err?.toUserMessage()}", Toast.LENGTH_LONG).show()
                return@launch
            }

            val categories = categoriesResult.getOrThrow().associateBy { it.id }
            val services = servicesResult.getOrThrow()
            val profile = profileResult.getOrThrow()

            b.etCity.setText(profile.city ?: "")
            b.etBio.setText(profile.bio ?: "")

            buildSpecializationList(services, categories.mapValues { it.value.name }, profile.specializationIds.toSet())
        }
    }

    private fun buildSpecializationList(
        services: List<ServiceResponse>,
        categoryNames: Map<Long, String>,
        checkedIds: Set<Long>
    ) {
        b.llSpecializations.removeAllViews()
        checkboxes.clear()

        val byCategory = services.filter { it.active }.groupBy { it.categoryId }
        byCategory.forEach { (categoryId, servicesInCategory) ->
            val header = TextView(requireContext()).apply {
                text = categoryNames[categoryId] ?: "Категория"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink))
                textSize = 14f
                setPadding(0, 16, 0, 6)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            b.llSpecializations.addView(header)

            servicesInCategory.forEach { service ->
                val checkBox = CheckBox(requireContext()).apply {
                    text = service.name
                    isChecked = checkedIds.contains(service.id)
                    tag = service.id
                    buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.mk_accent)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink))
                }
                checkboxes.add(checkBox)
                b.llSpecializations.addView(checkBox)
            }
        }

        if (services.isEmpty()) {
            b.llSpecializations.addView(TextView(requireContext()).apply {
                text = "Каталог услуг пуст"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink_hint))
                textSize = 13f
            })
        }
    }

    private fun save() {
        val specializationIds = checkboxes.filter { it.isChecked }.map { it.tag as Long }
        val request = UpdateMasterProfileRequest(
            city = b.etCity.text?.toString()?.trim() ?: "",
            bio = b.etBio.text?.toString()?.trim() ?: "",
            specializationIds = specializationIds
        )

        b.btnSave.isClickable = false
        b.btnSave.alpha = 0.6f

        lifecycleScope.launch {
            runCatching { ApiClient.api.updateMasterProfile(request) }
                .onSuccess {
                    Toast.makeText(requireContext(), getString(R.string.master_profile_saved), Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
            b.btnSave.isClickable = true
            b.btnSave.alpha = 1f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
