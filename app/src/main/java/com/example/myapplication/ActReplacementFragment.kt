package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentActReplacementBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.MeterInfoRequest
import com.example.myapplication.network.ReplacementActRequest
import com.example.myapplication.network.TaskStatusUpdateRequest
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ActReplacementFragment : Fragment() {

    private var _b: FragmentActReplacementBinding? = null
    private val b get() = _b!!
    private var taskId: Long = 0L
    private var addressId: Long = 0L
    private var selectedDate: String = ""
    private val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfUi  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentActReplacementBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId    = arguments?.getLong("taskId")    ?: 0L
        addressId = arguments?.getLong("addressId") ?: 0L

        (activity as? MainActivity)?.setDrawerEnabled(false)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.act_replacement_title))
        (activity as? MainActivity)?.setNotificationIconVisible(false)

        b.llDatePicker2.setOnClickListener { showDatePicker() }
        b.btnSave.setOnClickListener { submitAct() }

        loadAddressDisplay()
    }

    private fun loadAddressDisplay() {
        lifecycleScope.launch {
            // Если addressId не передан — подгружаем первый из словаря
            if (addressId == 0L) {
                runCatching { ApiClient.api.getAddresses() }
                    .onSuccess { list ->
                        addressId = list.firstOrNull()?.id ?: 0L
                        val addr = list.firstOrNull()
                        b.tvAddressValue.text = addr?.let {
                            listOfNotNull(it.street, it.house, it.apartment).joinToString(", ")
                        } ?: "—"
                    }
            } else {
                runCatching { ApiClient.api.getAddresses() }
                    .onSuccess { list ->
                        val addr = list.find { it.id == addressId }
                        b.tvAddressValue.text = addr?.let {
                            listOfNotNull(it.street, it.house, it.apartment).joinToString(", ")
                        } ?: "—"
                    }
            }
        }
    }

    private fun showDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Дата установки")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build().also { picker ->
                picker.addOnPositiveButtonClickListener { millis ->
                    val date = Date(millis)
                    selectedDate = sdfApi.format(date)
                    b.etInstallDate2.text = sdfUi.format(date)
                }
                picker.show(parentFragmentManager, "DATE")
            }
    }

    private fun submitAct() {
        val account   = b.etAccount.text?.toString()?.trim() ?: ""
        val oldBrand  = b.etOldBrand.text?.toString()?.trim() ?: ""
        val oldNumber = b.etOldNumber.text?.toString()?.trim() ?: ""
        val oldReads  = b.etOldReadings.text?.toString()?.toDoubleOrNull() ?: 0.0
        val newBrand  = b.etNewBrand.text?.toString()?.trim() ?: ""
        val newReads  = b.etNewReadings.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (account.isEmpty()) {
            Toast.makeText(requireContext(), "Введите номер лицевого счёта", Toast.LENGTH_SHORT).show()
            return
        }
        if (oldBrand.isEmpty() || newBrand.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните данные счётчиков", Toast.LENGTH_SHORT).show()
            return
        }
        if (addressId == 0L) {
            Toast.makeText(requireContext(), "Адрес не определён", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ReplacementActRequest(
            taskId           = taskId,
            addressId        = addressId,
            accountNumber    = account,
            installationDate = selectedDate.ifEmpty { null },
            oldMeter         = MeterInfoRequest(oldBrand, oldNumber.ifEmpty { "—" }, oldReads),
            newMeter         = MeterInfoRequest(newBrand, "NEW-${taskId}", newReads)
        )

        setLoading(true)
        lifecycleScope.launch {
            runCatching { ApiClient.api.createReplacementAct(request) }
                .onSuccess {
                    // После создания акта — завершаем задачу
                    runCatching {
                        ApiClient.api.updateTaskStatus(taskId, TaskStatusUpdateRequest("COMPLETED"))
                    }
                    Toast.makeText(requireContext(), "Акт замены создан и задача завершена", Toast.LENGTH_SHORT).show()
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
        b.btnSave.text = if (on) "Сохранение…" else "Сохранить акт"
        b.btnSave.alpha = if (on) 0.6f else 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
