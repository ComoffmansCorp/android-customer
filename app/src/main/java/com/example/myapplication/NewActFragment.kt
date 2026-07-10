package com.example.myapplication

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.DialogAddMeterBinding
import com.example.myapplication.databinding.FragmentNewActBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.InspectionActRequest
import com.example.myapplication.network.InspectionActResponse
import com.example.myapplication.network.MeterRequest
import com.example.myapplication.network.MeterResponse
import com.example.myapplication.network.PhotoResponse
import com.example.myapplication.network.toUserMessage
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewActFragment : Fragment() {

    private var _b: FragmentNewActBinding? = null
    private val b get() = _b!!
    private var taskId: Long = 0L
    private var selectedDate: String = ""
    private var act: InspectionActResponse? = null
    private var taskStatus: String? = null
    private val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfUi  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private val photoHelper = PhotoCaptureHelper(this) { loadPhotos() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentNewActBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId = arguments?.getLong("taskId") ?: 0L

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.app_name))
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        val types = arrayOf("Плановый", "Внеплановый")
        b.etInspectionType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        )
        b.etInspectionType.setText(types[0], false)

        selectedDate = sdfApi.format(Date())
        b.tvDate.text = sdfUi.format(Date())
        b.llDatePicker.setOnClickListener { showDatePicker() }

        b.btnAddMeter.setOnClickListener { showAddMeterDialog() }
        b.btnAddPhoto.setOnClickListener { photoHelper.showChooser() }
        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.btnSave.setOnClickListener { submitAct() }
        b.btnFinishTask.setOnClickListener { finishTask() }

        loadTaskAndAct()
    }

    private fun loadTaskAndAct() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getTask(taskId) }
                .onSuccess { t ->
                    b.tvAddress.text = t.addressLabel ?: "—"
                    taskStatus = t.status
                }
                .onFailure { b.tvAddress.text = "—" }

            // 404 here just means the act hasn't been created yet — that's
            // the normal "create" state, not an error to surface.
            runCatching { ApiClient.api.getInspectionActByTask(taskId) }
                .onSuccess { existing ->
                    act = existing
                    enterActExistsMode(existing)
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

    private fun submitAct() {
        val inspType = when (b.etInspectionType.text?.toString()) {
            "Плановый"    -> "SCHEDULED"
            "Внеплановый" -> "UNSCHEDULED"
            else          -> "SCHEDULED"
        }
        val notes = b.etRemarks.text?.toString()?.trim()

        val request = InspectionActRequest(
            taskId         = taskId,
            inspectionDate = selectedDate.ifEmpty { null },
            inspectionType = inspType,
            notes          = notes?.ifEmpty { null }
        )

        setLoading(true)
        lifecycleScope.launch {
            runCatching { ApiClient.api.createInspectionAct(request) }
                .onSuccess { created ->
                    act = created
                    setLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Акт создан. Добавьте приборы и фото, затем завершите наряд.",
                        Toast.LENGTH_LONG
                    ).show()
                    enterActExistsMode(created)
                }
                .onFailure {
                    setLoading(false)
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
        }
    }

    /** The backend has no "edit act" flow from this screen — once created,
     * the form locks and the meters/photos/finish sections take over. */
    private fun enterActExistsMode(a: InspectionActResponse) {
        b.etInspectionType.isEnabled = false
        b.llDatePicker.isClickable = false
        b.etRemarks.isEnabled = false
        b.btnSave.visibility = View.GONE

        b.cardMeters.visibility = View.VISIBLE
        b.cardPhotos.visibility = View.VISIBLE
        b.btnFinishTask.visibility = if (taskStatus == "IN_PROGRESS") View.VISIBLE else View.GONE

        photoHelper.inspectionActId = a.id

        loadMeters()
        loadPhotos()
    }

    private fun loadMeters() {
        val actId = act?.id ?: return
        lifecycleScope.launch {
            runCatching { ApiClient.api.getMeters(actId) }
                .onSuccess { meters -> renderMeters(meters) }
                .onFailure { Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    private fun renderMeters(meters: List<MeterResponse>) {
        b.llMetersList.removeAllViews()
        if (meters.isEmpty()) {
            b.llMetersList.addView(TextView(requireContext()).apply {
                text = "Приборы ещё не добавлены"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
                textSize = 13f
            })
            return
        }
        for (m in meters) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            row.addView(TextView(requireContext()).apply {
                text = "${meterTypeLabel(m.type)} · № ${m.serialNumber}"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                textSize = 14f
            })
            val details = listOfNotNull(
                m.manufactureYear?.let { "год $it" },
                m.verificationDate?.let { "поверка $it" },
                m.sealState?.let { sealLabel(it) }?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                row.addView(TextView(requireContext()).apply {
                    text = details
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    textSize = 12f
                })
            }
            b.llMetersList.addView(row)
        }
    }

    private fun meterTypeLabel(type: String) = when (type) {
        "SINGLE_PHASE" -> "Однофазный"
        "THREE_PHASE_DIRECT" -> "Трёхфазный прямого включения"
        "THREE_PHASE_TRANSFORMER" -> "Трёхфазный трансформаторный"
        else -> type
    }

    private fun sealLabel(state: String) = when (state) {
        "INTACT" -> "пломба не нарушена"
        "BROKEN" -> "пломба нарушена"
        "MISSING" -> "пломба отсутствует"
        else -> ""
    }

    private fun showAddMeterDialog() {
        val actId = act?.id ?: return
        val dialogBinding = DialogAddMeterBinding.inflate(LayoutInflater.from(requireContext()))

        val types = arrayOf("Однофазный", "Трёхфазный прямого включения", "Трёхфазный трансформаторный")
        val typeValues = arrayOf("SINGLE_PHASE", "THREE_PHASE_DIRECT", "THREE_PHASE_TRANSFORMER")
        dialogBinding.etMeterType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        )
        dialogBinding.etMeterType.setText(types[0], false)

        val seals = arrayOf("Не указано", "Не нарушена", "Нарушена", "Отсутствует")
        val sealValues = arrayOf(null, "INTACT", "BROKEN", "MISSING")
        dialogBinding.etMeterSeal.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, seals)
        )
        dialogBinding.etMeterSeal.setText(seals[0], false)

        var meterDate = ""
        dialogBinding.llMeterDatePicker.setOnClickListener {
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Дата поверки")
                .build().also { picker ->
                    picker.addOnPositiveButtonClickListener { millis ->
                        val date = Date(millis)
                        meterDate = sdfApi.format(date)
                        dialogBinding.tvMeterDate.text = sdfUi.format(date)
                    }
                    picker.show(parentFragmentManager, "METER_DATE")
                }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Новый прибор учёта")
            .setView(dialogBinding.root)
            .setPositiveButton("Добавить") { _, _ ->
                val serial = dialogBinding.etMeterSerial.text?.toString()?.trim().orEmpty()
                if (serial.isEmpty()) {
                    Toast.makeText(requireContext(), "Укажите серийный номер", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val typeIndex = types.indexOf(dialogBinding.etMeterType.text?.toString()).takeIf { it >= 0 } ?: 0
                val sealIndex = seals.indexOf(dialogBinding.etMeterSeal.text?.toString()).takeIf { it >= 0 } ?: 0
                val request = MeterRequest(
                    type = typeValues[typeIndex],
                    serialNumber = serial,
                    manufactureYear = dialogBinding.etMeterYear.text?.toString()?.toIntOrNull(),
                    verificationDate = meterDate.ifEmpty { null },
                    sealState = sealValues[sealIndex],
                    transformationRatio = dialogBinding.etMeterRatio.text?.toString()?.toIntOrNull()
                )
                lifecycleScope.launch {
                    runCatching { ApiClient.api.addMeter(actId, request) }
                        .onSuccess { loadMeters() }
                        .onFailure { Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun loadPhotos() {
        val actId = act?.id ?: return
        lifecycleScope.launch {
            runCatching { ApiClient.api.getPhotosForInspection(actId) }
                .onSuccess { photos -> renderPhotos(photos) }
                .onFailure { Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    private fun renderPhotos(photos: List<PhotoResponse>) {
        b.llPhotosList.removeAllViews()
        val size = dp(80)
        for (p in photos) {
            val thumb = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginEnd = dp(8) }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.input_bg))
            }
            b.llPhotosList.addView(thumb)
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        ApiClient.api.downloadPhoto(p.id).byteStream().use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                }
                if (_b != null && bitmap != null) thumb.setImageBitmap(bitmap)
            }
        }
    }

    private fun finishTask() {
        AlertDialog.Builder(requireContext())
            .setTitle("Завершить наряд")
            .setMessage("Акт заполнен. Завершить наряд?")
            .setPositiveButton("Завершить") { _, _ ->
                lifecycleScope.launch {
                    runCatching { ApiClient.api.completeTask(taskId) }
                        .onSuccess {
                            Toast.makeText(requireContext(), "Наряд завершён", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack(R.id.taskListFragment, false)
                        }
                        .onFailure { Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
