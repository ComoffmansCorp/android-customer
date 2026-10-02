package com.example.myapplication

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemRequestCardBinding
import com.example.myapplication.network.ServiceRequestResponse

class ServiceRequestAdapter(
    val requests: MutableList<ServiceRequestResponse>,
    /** MyRequestsFragment already owns every item shown here — hide the claim action entirely. */
    private val showClaimButton: Boolean,
    private val onClaim: (ServiceRequestResponse) -> Unit,
    private val onDetails: (ServiceRequestResponse) -> Unit
) : RecyclerView.Adapter<ServiceRequestAdapter.VH>() {

    inner class VH(private val b: ItemRequestCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(request: ServiceRequestResponse) {
            val ctx = b.root.context

            b.tvRequestMeta.text = "Заказ №${request.id}"
            b.tvRequestPrice.text = request.agreedPrice?.let { java.text.NumberFormat.getIntegerInstance(java.util.Locale("ru")).format(it) + " ₽" } ?: "Предложите свою цену"
            b.root.setOnClickListener { onDetails(request) }
            b.tvRequestTitle.text = request.serviceName ?: "Услуга"
            b.tvRequestSubtitle.text = buildString {
                append(request.addressText)
                if (request.description.isNotBlank()) {
                    append("\n")
                    append(request.description.take(80))
                }
            }

            val style = when (request.status) {
                "OPEN"      -> StatusStyle(R.color.mk_status_open_fg, R.drawable.bg_mk_status_open, R.color.mk_status_open_fg, R.drawable.ic_status_open, ctx.getString(R.string.status_open_str))
                "ASSIGNED"  -> StatusStyle(R.color.mk_status_progress_fg, R.drawable.bg_mk_status_progress, R.color.mk_status_progress_fg, R.drawable.ic_status_assigned, ctx.getString(R.string.status_in_progress_str))
                "COMPLETED" -> StatusStyle(R.color.mk_status_done_fg, R.drawable.bg_mk_status_done, R.color.mk_status_done_fg, R.drawable.ic_status_done, ctx.getString(R.string.status_done_str))
                "CANCELED"  -> StatusStyle(R.color.mk_status_canceled_fg, R.drawable.bg_mk_status_canceled, R.color.mk_status_canceled_fg, R.drawable.ic_status_canceled, ctx.getString(R.string.status_canceled_str))
                else        -> StatusStyle(R.color.mk_status_open_fg, R.drawable.bg_mk_status_open, R.color.mk_status_open_fg, R.drawable.ic_status_open, request.status)
            }

            b.statusStrip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, style.stripColorRes))
            b.ivStatusBg.setBackgroundResource(style.badgeBgRes)
            b.ivStatusIcon.setImageResource(style.iconRes)
            b.ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, style.fgColorRes))
            b.tvStatusBadge.text = style.label
            b.tvStatusBadge.setBackgroundResource(style.badgeBgRes)
            b.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, style.fgColorRes))

            if (showClaimButton && request.status == "OPEN") {
                b.btnClaim.visibility = View.VISIBLE
                b.btnClaim.setOnClickListener { onClaim(request) }
            } else {
                b.btnClaim.visibility = View.GONE
            }

            b.btnDetails.setOnClickListener { onDetails(request) }
        }
    }

    private data class StatusStyle(
        val stripColorRes: Int,
        val badgeBgRes: Int,
        val fgColorRes: Int,
        val iconRes: Int,
        val label: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRequestCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(requests[position])
    override fun getItemCount() = requests.size

    fun updateItem(request: ServiceRequestResponse) {
        val i = requests.indexOfFirst { it.id == request.id }
        if (i >= 0) { requests[i] = request; notifyItemChanged(i) }
    }

    fun removeItem(id: Long) {
        val i = requests.indexOfFirst { it.id == id }
        if (i >= 0) { requests.removeAt(i); notifyItemRemoved(i) }
    }

    fun replaceAll(newRequests: List<ServiceRequestResponse>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }
}
