package com.example.myapplication

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentRequestDetailBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.MessageResponse
import com.example.myapplication.network.SendMessageRequest
import com.example.myapplication.network.ServiceRequestResponse
import com.example.myapplication.network.SubmitOfferRequest
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class RequestDetailFragment : Fragment() {

    private var _b: FragmentRequestDetailBinding? = null
    private val b get() = _b!!
    private var request: ServiceRequestResponse? = null
    private var chatPollJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentRequestDetailBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requestId = arguments?.getLong("requestId") ?: return
        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        b.btnBack.setOnClickListener { findNavController().popBackStack() }

        loadRequest(requestId)
    }

    private fun loadRequest(id: Long) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getServiceRequest(id) }
                .onSuccess { r -> request = r; bind(r) }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun bind(r: ServiceRequestResponse) {
        (activity as? MainActivity)?.setToolbarTitle(r.serviceName ?: "Заявка")

        b.tvServicePill.text = r.serviceName ?: "Услуга"
        b.tvAddress.text = r.addressText
        b.tvClient.text = "Клиент #${r.clientId}"
        b.tvDescription.text = r.description.takeIf { it.isNotBlank() } ?: "—"

        applyStatus(r.status)
        updateButtons(r)

        if (r.masterId != null) {
            b.cardChat.visibility = View.VISIBLE
            b.btnSendMessage.setOnClickListener { sendMessage(r) }
            startChatPolling(r.id)
        } else {
            b.cardChat.visibility = View.GONE
        }
    }

    /** Polls every 5s while the view is visible -- bound to
     * viewLifecycleOwner so it's cancelled automatically on destroy, and
     * only one loop ever runs even if bind() is called again after a
     * status change. */
    private fun startChatPolling(requestId: Long) {
        chatPollJob?.cancel()
        chatPollJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                runCatching { ApiClient.api.getMessages(requestId) }
                    .onSuccess { renderMessages(it) }
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    private fun renderMessages(messages: List<MessageResponse>) {
        val myUserId = AuthManager.userId
        b.llChatMessages.removeAllViews()
        messages.forEach { m ->
            val bubble = TextView(requireContext()).apply {
                text = m.text
                textSize = 13f
                setPadding(28, 18, 28, 18)
                val mine = m.senderId == myUserId
                setBackgroundResource(if (mine) R.drawable.bg_mk_btn_primary else R.drawable.bg_input)
                setTextColor(ContextCompat.getColor(requireContext(), if (mine) android.R.color.white else R.color.mk_ink))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = 12
                lp.gravity = if (mine) android.view.Gravity.END else android.view.Gravity.START
                layoutParams = lp
            }
            b.llChatMessages.addView(bubble)
        }
    }

    private fun sendMessage(r: ServiceRequestResponse) {
        val text = b.etChatMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        lifecycleScope.launch {
            runCatching { ApiClient.api.sendMessage(r.id, SendMessageRequest(text)) }
                .onSuccess {
                    b.etChatMessage.setText("")
                    runCatching { ApiClient.api.getMessages(r.id) }.onSuccess { renderMessages(it) }
                }
                .onFailure { Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateButtons(r: ServiceRequestResponse) {
        when (r.status) {
            "OPEN" -> {
                b.btnClaimWork.visibility = View.VISIBLE
                b.btnClaimWork.isClickable = true
                b.btnClaimWork.alpha = 1f
                b.btnClaimWork.setOnClickListener { showOfferDialog(r) }
                b.btnCompleteRequest.visibility = View.GONE
            }
            "ASSIGNED" -> {
                b.btnClaimWork.visibility = View.GONE
                b.btnCompleteRequest.visibility = View.VISIBLE
                b.btnCompleteRequest.isClickable = true
                b.btnCompleteRequest.alpha = 1f
                b.btnCompleteRequest.setOnClickListener { confirmComplete(r) }
            }
            else -> {
                b.btnClaimWork.visibility = View.GONE
                b.btnCompleteRequest.visibility = View.GONE
            }
        }
    }

    /** Bidding: propose a price/comment -- the client (web-only) accepts one
     * offer, this fragment doesn't flip straight to ASSIGNED on submit. */
    private fun showOfferDialog(r: ServiceRequestResponse) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val priceInput = EditText(requireContext()).apply {
            hint = "Цена, ₽"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val commentInput = EditText(requireContext()).apply { hint = "Комментарий (необязательно)" }
        container.addView(priceInput)
        container.addView(commentInput)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.btn_claim_work))
            .setView(container)
            .setPositiveButton("Отправить") { _, _ ->
                val price = priceInput.text?.toString()?.trim()?.toDoubleOrNull()
                if (price == null || price <= 0) {
                    Toast.makeText(requireContext(), "Укажите корректную цену", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                submitOffer(r, price, commentInput.text?.toString()?.trim().orEmpty())
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun submitOffer(r: ServiceRequestResponse, price: Double, comment: String) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.submitOffer(r.id, SubmitOfferRequest(price, comment)) }
                .onSuccess {
                    Toast.makeText(requireContext(), "Отклик отправлен", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun confirmComplete(r: ServiceRequestResponse) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Завершить заявку")
            .setMessage("Работа выполнена. Завершить заявку?")
            .setPositiveButton("Завершить") { _, _ -> complete(r) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun complete(r: ServiceRequestResponse) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.completeRequest(r.id) }
                .onSuccess { updated ->
                    request = updated
                    applyStatus(updated.status)
                    updateButtons(updated)
                    Toast.makeText(requireContext(), "Заявка завершена", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun applyStatus(status: String) {
        val (text, bgRes, fgRes) = when (status) {
            "OPEN"     -> Triple(getString(R.string.status_open_str), R.color.mk_status_open_bg, R.color.mk_status_open_fg)
            "ASSIGNED" -> Triple(getString(R.string.status_in_progress_str), R.color.mk_status_progress_bg, R.color.mk_status_progress_fg)
            "COMPLETED"   -> Triple(getString(R.string.status_done_str), R.color.mk_status_done_bg, R.color.mk_status_done_fg)
            "CANCELED"    -> Triple(getString(R.string.status_canceled_str), R.color.mk_status_canceled_bg, R.color.mk_status_canceled_fg)
            else          -> Triple(status, R.color.mk_status_open_bg, R.color.mk_status_open_fg)
        }
        b.tvStatus.text = text
        b.tvStatus.setBackgroundColor(ContextCompat.getColor(requireContext(), bgRes))
        b.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), fgRes))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
