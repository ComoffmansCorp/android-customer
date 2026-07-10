package com.example.myapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemTaskCardBinding
import com.example.myapplication.network.TaskResponse

class NetworkTaskAdapter(
    val tasks: MutableList<TaskResponse>,
    private val onAccept: (TaskResponse) -> Unit,
    private val onDetails: (TaskResponse) -> Unit
) : RecyclerView.Adapter<NetworkTaskAdapter.VH>() {

    inner class VH(private val b: ItemTaskCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(task: TaskResponse) {
            val typeLabel = when (task.type) {
                "INSPECTION"  -> "Осмотр оборудования"
                "REPLACEMENT" -> "Замена оборудования"
                else          -> task.type
            }
            b.tvTaskTitle.text = "$typeLabel — до ${task.dueDate ?: "—"}"
            b.tvTaskSubtitle.text = buildString {
                append(task.addressLabel ?: "Адрес не указан")
                if (!task.assigneeName.isNullOrBlank()) append("\nИнспектор: ${task.assigneeName}")
            }

            val (strip, bgColor, iconTint, statusText, statusBg) = when (task.status) {
                "PENDING"     -> StatusStyle("#F59E0B", "#FEF3C7", "#92400E", "Ожидает",  "#FEF3C7")
                "IN_PROGRESS" -> StatusStyle("#3B82F6", "#DBEAFE", "#1E3A8A", "В работе", "#DBEAFE")
                "COMPLETED"   -> StatusStyle("#10B981", "#D1FAE5", "#064E3B", "Выполнено","#D1FAE5")
                "CANCELED"    -> StatusStyle("#9CA3AF", "#F3F4F6", "#374151", "Отменено", "#F3F4F6")
                else          -> StatusStyle("#9CA3AF", "#F3F4F6", "#374151", task.status, "#F3F4F6")
            }

            b.statusStrip.setBackgroundColor(Color.parseColor(strip))
            b.ivStatusBg.setBackgroundColor(Color.parseColor(bgColor))
            b.ivStatusIcon.imageTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor(iconTint))
            b.tvStatusBadge.text = statusText
            b.tvStatusBadge.setBackgroundColor(Color.parseColor(statusBg))
            b.tvStatusBadge.setTextColor(Color.parseColor(iconTint))

            // Принять — только если PENDING
            if (task.status == "PENDING") {
                b.btnAccept.visibility = View.VISIBLE
                b.btnAccept.setOnClickListener { onAccept(task) }
            } else {
                b.btnAccept.visibility = View.INVISIBLE
            }

            b.btnDetails.setOnClickListener { onDetails(task) }
        }
    }

    private data class StatusStyle(
        val strip: String,
        val bgColor: String,
        val iconTint: String,
        val statusText: String,
        val statusBg: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemTaskCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(tasks[position])
    override fun getItemCount() = tasks.size

    fun updateItem(task: TaskResponse) {
        val i = tasks.indexOfFirst { it.id == task.id }
        if (i >= 0) { tasks[i] = task; notifyItemChanged(i) }
    }

    fun replaceAll(newTasks: List<TaskResponse>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }
}
