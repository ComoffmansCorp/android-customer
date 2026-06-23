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
import com.example.myapplication.network.TaskStatusUpdateRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class OrderDetailFragment : Fragment() {

    private var _b: FragmentOrderDetailBinding? = null
    private val b get() = _b!!
    private var task: TaskResponse? = null
    private var addressId: Long = 0L

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

        loadTaskAndAddress(taskId)
    }

    private fun loadTaskAndAddress(taskId: Long) {
        lifecycleScope.launch {
            // Параллельно загружаем задачи и адреса
            val tasksDeferred = async { runCatching { ApiClient.api.getTasks() } }
            val addressesDeferred = async { runCatching { ApiClient.api.getAddresses() } }

            val tasksResult = tasksDeferred.await()
            val addressesResult = addressesDeferred.await()

            tasksResult.onSuccess { tasks ->
                val found = tasks.find { it.id == taskId }
                if (found == null) {
                    Toast.makeText(requireContext(), "Задача не найдена", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                task = found
                bindTask(found)

                // Ищем совпадение по адресу
                addressesResult.onSuccess { addrs ->
                    val match = addrs.firstOrNull { addr ->
                        val full = listOfNotNull(addr.street, addr.house, addr.apartment)
                            .joinToString(", ")
                        found.address.contains(addr.street ?: "", ignoreCase = true) ||
                        full.contains(addr.house ?: "", ignoreCase = true)
                    }
                    addressId = match?.id ?: addrs.firstOrNull()?.id ?: 0L
                }
            }
            tasksResult.onFailure {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindTask(t: TaskResponse) {
        val typeLabel = when (t.type) {
            "INSPECTION"  -> "Осмотр оборудования"
            "REPLACEMENT" -> "Замена оборудования"
            else -> t.type
        }
        (activity as? MainActivity)?.setToolbarTitle("Заказ — до ${t.dueDate ?: ""}")

        b.tvTaskTypePill.text = typeLabel
        b.tvAddress.text = t.address
        b.tvType.text = typeLabel
        b.tvDeadline.text = t.dueDate ?: "—"
        b.tvNotes.text = if (!t.consumerName.isNullOrBlank()) t.consumerName else "—"

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
                b.btnAcceptWork.background = requireContext()
                    .getDrawable(R.drawable.bg_btn_primary)
                b.btnCreateAct.visibility = View.GONE
                b.btnAcceptWork.setOnClickListener { acceptTask(t) }
            }
            "IN_PROGRESS" -> {
                b.btnAcceptWork.visibility = View.VISIBLE
                b.btnAcceptWork.isClickable = false
                b.btnAcceptWork.alpha = 0.5f
                b.btnAcceptWork.text = "✓  Принято в работу"

                b.btnCreateAct.visibility = View.VISIBLE
                b.btnCreateAct.text = when (t.type) {
                    "REPLACEMENT" -> "Создать акт замены"
                    else          -> "Создать акт осмотра"
                }
                b.btnCreateAct.setOnClickListener { openActForm(t) }

                b.btnCompleteTask.visibility = View.VISIBLE
                b.btnCompleteTask.setOnClickListener { completeTask(t) }
            }
            "COMPLETED" -> {
                b.btnAcceptWork.visibility = View.VISIBLE
                b.btnAcceptWork.isClickable = false
                b.btnAcceptWork.alpha = 0.5f
                b.btnAcceptWork.text = "✓  Выполнено"
                b.btnCreateAct.visibility = View.GONE
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
            runCatching {
                ApiClient.api.updateTaskStatus(t.id, TaskStatusUpdateRequest("IN_PROGRESS"))
            }.onSuccess { updated ->
                task = updated
                applyStatus(updated.status)
                updateButtons(updated)
                Toast.makeText(requireContext(), "Задача принята в работу!", Toast.LENGTH_SHORT).show()
            }.onFailure {
                b.btnAcceptWork.isClickable = true
                b.btnAcceptWork.alpha = 1f
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun completeTask(t: TaskResponse) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Завершить задачу")
            .setMessage("Завершить задачу без создания акта?")
            .setPositiveButton("Завершить") { _, _ ->
                lifecycleScope.launch {
                    runCatching {
                        ApiClient.api.updateTaskStatus(t.id, TaskStatusUpdateRequest("COMPLETED"))
                    }.onSuccess { updated ->
                        task = updated
                        applyStatus(updated.status)
                        updateButtons(updated)
                        Toast.makeText(requireContext(), "Задача завершена", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
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
        findNavController().navigate(dest, bundleOf(
            "taskId"    to t.id,
            "addressId" to addressId
        ))
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
