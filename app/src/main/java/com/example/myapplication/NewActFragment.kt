package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentNewActBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.InspectionActRequest
import com.example.myapplication.network.TaskStatusUpdateRequest
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NewActFragment : Fragment() {

    private var _b: FragmentNewActBinding? = null
    private val b get() = _b!!
    private var taskId: Long = 0L
    private var addressId: Long = 0L
    private var selectedDate: String = ""
    private val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfUi  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentNewActBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId    = arguments?.getLong("taskId")    ?: 0L
        addressId = arguments?.getLong("addressId") ?: 0L

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.app_name))
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        // Dropdown типов осмотра
        val types = arrayOf("Плановый", "Внеплановый")
        b.etInspectionType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        )
        b.etInspectionType.setText(types[0], false)

        // Дата по умолчанию — сегодня
        selectedDate = sdfApi.format(Date())
        b.tvDate.text = sdfUi.format(Date())
        b.llDatePicker.setOnClickListener { showDatePicker() }

        b.btnMeters.setOnClickListener { loadAndShowMeters() }
        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.btnSave.setOnClickListener { submitAct() }

        // Загрузить адрес для отображения
        if (addressId == 0L) {
            resolveAddress()
        } else {
            loadAddressDisplay()
        }
    }

    private fun resolveAddress() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getAddresses() }
                .onSuccess { list ->
                    if (addressId == 0L) addressId = list.firstOrNull()?.id ?: 0L
                    val addr = list.find { it.id == addressId }
                    b.tvAddress.text = addr?.let {
                        listOfNotNull(it.street, it.house, it.apartment).joinToString(", ")
                    } ?: "Адрес не найден"
                }
        }
    }

    private fun loadAddressDisplay() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getAddresses() }
                .onSuccess { list ->
                    val addr = list.find { it.id == addressId }
                    b.tvAddress.text = addr?.let {
                        listOfNotNull(it.street, it.house, it.apartment).joinToString(", ")
                    } ?: "—"
                }
        }
    }

    private fun showDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Дата осмотра")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build().also { picker ->
                picker.addOnPositiveButtonClickListener { millis ->
                    val date = Date(millis)
                    selectedDate = sdfApi.format(date)
                    b.tvDate.text = sdfUi.format(date)
                }
                picker.show(parentFragmentManager, "DATE")
            }
    }

    private fun loadAndShowMeters() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getAddresses() }
                .onSuccess { addrs ->
                    val addr = addrs.find { it.id == addressId }
                    val info = if (addr != null)
                        "Адрес: ${listOfNotNull(addr.street, addr.house, addr.apartment).joinToString(", ")}\n" +
                        "Потребитель: ${addr.consumer ?: "—"}\n\nСчётчики будут доступны после интеграции с реестром оборудования."
                    else "Адрес не определён"
                    AlertDialog.Builder(requireContext())
                        .setTitle("Счётчики")
                        .setMessage(info)
                        .setPositiveButton("Закрыть", null)
                        .show()
                }
        }
    }

    private fun submitAct() {
        if (addressId == 0L) {
            Toast.makeText(requireContext(), "Не определён адрес", Toast.LENGTH_SHORT).show()
            return
        }

        val inspType = when (b.etInspectionType.text?.toString()) {
            "Плановый"    -> "SCHEDULED"
            "Внеплановый" -> "UNSCHEDULED"
            else          -> "SCHEDULED"
        }
        val consumer = b.etConsumer.text?.toString()?.trim()
        val notes    = b.etRemarks.text?.toString()?.trim()

        val request = InspectionActRequest(
            taskId         = taskId,
            addressId      = addressId,
            inspectionDate = selectedDate.ifEmpty { null },
            inspectionType = inspType,
            notes          = notes?.ifEmpty { null }
        )

        setLoading(true)
        lifecycleScope.launch {
            runCatching { ApiClient.api.createInspectionAct(request) }
                .onSuccess {
                    // После создания акта — завершаем задачу
                    runCatching {
                        ApiClient.api.updateTaskStatus(taskId, TaskStatusUpdateRequest("COMPLETED"))
                    }
                    Toast.makeText(requireContext(), "Акт осмотра создан и задача завершена", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.taskListFragment, false)
                }
                .onFailure {
                    setLoading(false)
                    Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(on: Boolean) {
        b.btnSave.isClickable = !on
        b.btnSave.text = if (on) "Сохранение…" else "Сохранить"
        b.btnSave.alpha = if (on) 0.6f else 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
