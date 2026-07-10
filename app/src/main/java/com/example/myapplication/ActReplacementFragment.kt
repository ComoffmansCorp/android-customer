package com.example.myapplication

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentActReplacementBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.PhotoResponse
import com.example.myapplication.network.ReplacementActRequest
import com.example.myapplication.network.ReplacementActResponse
import com.example.myapplication.network.toUserMessage
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActReplacementFragment : Fragment() {

    private var _b: FragmentActReplacementBinding? = null
    private val b get() = _b!!
    private var taskId: Long = 0L
    private var selectedDate: String = ""
    private var act: ReplacementActResponse? = null
    private var taskStatus: String? = null
    private val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfUi  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    private val photoHelper = PhotoCaptureHelper(this) { loadPhotos() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentActReplacementBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId = arguments?.getLong("taskId") ?: 0L

        (activity as? MainActivity)?.setDrawerEnabled(false)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.act_replacement_title))
        (activity as? MainActivity)?.setNotificationIconVisible(false)

        b.llDatePicker2.setOnClickListener { showDatePicker() }
        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        b.btnSave.setOnClickListener { submitAct() }
        b.btnAddPhoto.setOnClickListener { photoHelper.showChooser() }
        b.btnFinishTask.setOnClickListener { finishTask() }

        loadTaskAndAct()
    }

    private fun loadTaskAndAct() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getTask(taskId) }
                .onSuccess { t ->
                    b.tvAddressValue.text = t.addressLabel ?: "—"
                    taskStatus = t.status
                }
                .onFailure { b.tvAddressValue.text = "—" }

            // 404 here just means no act yet — normal "create" state.
            runCatching { ApiClient.api.getReplacementActByTask(taskId) }
                .onSuccess { existing ->
                    act = existing
                    bindExisting(existing)
                    enterActExistsMode()
                }
        }
    }

    private fun bindExisting(a: ReplacementActResponse) {
        b.etAccount.setText(a.accountNumber)
        b.etOldBrand.setText(a.oldBrand)
        b.etOldNumber.setText(a.oldSerialNumber)
        b.etOldReadings.setText(a.oldReadings?.toString() ?: "")
        b.etNewBrand.setText(a.newBrand)
        b.etNewReadings.setText(a.newReadings?.toString() ?: "")
        a.installationDate?.let { iso ->
            selectedDate = iso
            runCatching { sdfUi.format(sdfApi.parse(iso)!!) }.getOrNull()?.let { b.etInstallDate2.text = it }
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
        val oldBrand  = b.etOldBrand.text?.toString()?.trim()
        val oldNumber = b.etOldNumber.text?.toString()?.trim()
        val oldReads  = b.etOldReadings.text?.toString()?.toDoubleOrNull()
        val newBrand  = b.etNewBrand.text?.toString()?.trim()
        val newReads  = b.etNewReadings.text?.toString()?.toDoubleOrNull()

        if (account.isEmpty()) {
            Toast.makeText(requireContext(), "Введите номер лицевого счёта", Toast.LENGTH_SHORT).show()
            return
        }

        val request = ReplacementActRequest(
            taskId           = taskId,
            accountNumber    = account,
            installationDate = selectedDate.ifEmpty { null },
            oldBrand         = oldBrand?.ifEmpty { null },
            oldSerialNumber  = oldNumber?.ifEmpty { null },
            oldReadings      = oldReads,
            newBrand         = newBrand?.ifEmpty { null },
            newSerialNumber  = null,
            newReadings      = newReads
        )

        setLoading(true)
        lifecycleScope.launch {
            runCatching { ApiClient.api.createReplacementAct(request) }
                .onSuccess { created ->
                    act = created
                    setLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Акт создан. Добавьте фото, затем завершите наряд.",
                        Toast.LENGTH_LONG
                    ).show()
                    enterActExistsMode()
                }
                .onFailure {
                    setLoading(false)
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
        }
    }

    /** No "edit act" flow from this screen — once created, the form locks
     * and the photos/finish sections take over. */
    private fun enterActExistsMode() {
        b.etAccount.isEnabled = false
        b.etOldBrand.isEnabled = false
        b.etOldNumber.isEnabled = false
        b.etOldReadings.isEnabled = false
        b.etNewBrand.isEnabled = false
        b.etNewReadings.isEnabled = false
        b.llDatePicker2.isClickable = false
        b.btnSave.visibility = View.GONE

        b.cardPhotos.visibility = View.VISIBLE
        b.btnFinishTask.visibility = if (taskStatus == "IN_PROGRESS") View.VISIBLE else View.GONE

        photoHelper.replacementActId = act?.id

        loadPhotos()
    }

    private fun loadPhotos() {
        val actId = act?.id ?: return
        lifecycleScope.launch {
            runCatching { ApiClient.api.getPhotosForReplacement(actId) }
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
        b.btnSave.text = if (on) "Сохранение…" else "Сохранить акт"
        b.btnSave.alpha = if (on) 0.6f else 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
