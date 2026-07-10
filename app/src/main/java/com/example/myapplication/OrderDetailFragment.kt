package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentOrderDetailBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.TaskResponse
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class OrderDetailFragment : Fragment() {

    private var _b: FragmentOrderDetailBinding? = null
    private val b get() = _b!!
    private var task: TaskResponse? = null
    private var hasAct: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskId = arguments?.getLong("taskId") ?: return
        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        loadTask(taskId)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the act screen (act just created, or meters/photos
        // added) — refresh so the act-exists gate on "Завершить наряд" updates.
        task?.let { refresh(it.id) }
    }

    private fun loadTask(taskId: Long) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getTask(taskId) }
                .onSuccess { t ->
                    task = t
                    bindTask(t)
                    checkActExists(t)
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun refresh(taskId: Long) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getTask(taskId) }
                .onSuccess { t ->
                    task = t
                    bindTask(t)
                    checkActExists(t)
                }
        }
    }

    private suspend fun checkActExists(t: TaskResponse) {
        hasAct = runCatching {
            if (t.type == "REPLACEMENT") ApiClient.api.getReplacementActByTask(t.id)
            else ApiClient.api.getInspectionActByTask(t.id)
        }.isSuccess
        updateButtons(t)
    }

    private fun bindTask(t: TaskResponse) {
        val typeLabel = when (t.type) {
            "INSPECTION"  -> "Осмотр оборудования"
            "REPLACEMENT" -> "Замена оборудования"
            else -> t.type
        }
        (activity as? MainActivity)?.setToolbarTitle("Наряд — до ${t.dueDate ?: ""}")

        b.tvTaskTypePill.text = typeLabel
        b.tvAddress.text = t.addressLabel ?: "—"
        b.tvType.text = typeLabel
        b.tvDeadline.text = t.dueDate ?: "—"
        b.tvNotes.text = t.cancelReason?.takeIf { it.isNotBlank() } ?: "—"

        applyStatus(t.status)
        updateButtons(t)
    }

    private fun updateButtons(t: TaskResponse) {
        when (t.status) {
            "PENDING" -> {
                b.btnAcceptWork.visibility = View.VISIBLE
                b.btnAcceptWork.isClickable = true
                b.btnAcceptWork.alpha = 1f
                b.btnAcceptWork.text = "Принять в работу"
                b.btnAcceptWork.background = requireContext().getDrawable(R.drawable.bg_btn_primary)
                b.btnAcceptWork.setOnClickListener { acceptTask(t) }
                b.btnCreateAct.visibility = View.GONE
                b.btnCompleteTask.visibility = View.GONE
            }
            "IN_PROGRESS" -> {
                b.btnAcceptWork.visibility = View.VISIBLE
                b.btnAcceptWork.isClickable = false
                b.btnAcceptWork.alpha = 0.5f
                b.btnAcceptWork.text = "✓  Принято в работу"

                b.btnCreateAct.visibility = View.VISIBLE
                b.btnCreateAct.text = when {
                    hasAct && t.type == "REPLACEMENT" -> "Открыть акт замены"
                    hasAct                             -> "Открыть акт осмотра"
                    t.type == "REPLACEMENT"            -> "Создать акт замены"
                    else                                -> "Создать акт осмотра"
                }
                b.btnCreateAct.setOnClickListener { openActForm(t) }

                b.btnCompleteTask.visibility = View.VISIBLE
                if (hasAct) {
                    b.btnCompleteTask.text = "Завершить наряд"
                    b.btnCompleteTask.alpha = 1f
                    b.btnCompleteTask.isClickable = true
                    b.btnCompleteTask.setOnClickListener { completeTask(t) }
                } else {
                    b.btnCompleteTask.text = "Сначала заполните акт"
                    b.btnCompleteTask.alpha = 0.5f
                    b.btnCompleteTask.isClickable = false
                    b.btnCompleteTask.setOnClickListener(null)
                }
            }
            "COMPLETED" -> {
                b.btnAcceptWork.visibility = View.VISIBLE
                b.btnAcceptWork.isClickable = false
                b.btnAcceptWork.alpha = 0.5f
                b.btnAcceptWork.text = "✓  Выполнено"
                b.btnCreateAct.visibility = View.VISIBLE
                b.btnCreateAct.text = "Открыть акт"
                b.btnCreateAct.setOnClickListener { openActForm(t) }
                b.btnCompleteTask.visibility = View.GONE
            }
            else -> {
                b.btnAcceptWork.visibility = View.GONE
                b.btnCreateAct.visibility = View.GONE
                b.btnCompleteTask.visibility = View.GONE
            }
        }
    }

    private fun acceptTask(t: TaskResponse) {
        b.btnAcceptWork.isClickable = false
        b.btnAcceptWork.alpha = 0.6f

        lifecycleScope.launch {
            runCatching { ApiClient.api.startTask(t.id) }
                .onSuccess { updated ->
                    task = updated
                    applyStatus(updated.status)
                    updateButtons(updated)
                    Toast.makeText(requireContext(), "Наряд взят в работу!", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    b.btnAcceptWork.isClickable = true
                    b.btnAcceptWork.alpha = 1f
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun completeTask(t: TaskResponse) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Завершить наряд")
            .setMessage("Акт заполнен. Завершить наряд?")
            .setPositiveButton("Завершить") { _, _ ->
                lifecycleScope.launch {
                    runCatching { ApiClient.api.completeTask(t.id) }
                        .onSuccess { updated ->
                            task = updated
                            applyStatus(updated.status)
                            updateButtons(updated)
                            Toast.makeText(requireContext(), "Наряд завершён", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                        }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openActForm(t: TaskResponse) {
        val dest = when (t.type) {
            "REPLACEMENT" -> R.id.action_orderDetail_to_actReplacement
            else          -> R.id.action_orderDetail_to_newAct
        }
        findNavController().navigate(dest, bundleOf("taskId" to t.id))
    }

    private fun applyStatus(status: String) {
        val (text, bg, fg) = when (status) {
            "PENDING"     -> Triple("Ждёт принятия", "#FEF3C7", "#92400E")
            "IN_PROGRESS" -> Triple("В работе",      "#DBEAFE", "#1E3A8A")
            "COMPLETED"   -> Triple("Выполнено",     "#D1FAE5", "#064E3B")
            "CANCELED"    -> Triple("Отменено",      "#FEE2E2", "#991B1B")
            else          -> Triple(status,           "#F3F4F6", "#374151")
        }
        b.tvStatus.text = text
        b.tvStatus.setBackgroundColor(Color.parseColor(bg))
        b.tvStatus.setTextColor(Color.parseColor(fg))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
