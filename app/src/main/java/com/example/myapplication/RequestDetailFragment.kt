package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentRequestDetailBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.ServiceRequestResponse
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class RequestDetailFragment : Fragment() {

    private var _b: FragmentRequestDetailBinding? = null
    private val b get() = _b!!
    private var request: ServiceRequestResponse? = null

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

        b.tvServicePill.text = buildString {
            append(r.serviceName ?: "Услуга")
            if (!r.categoryName.isNullOrBlank()) append(" · ${r.categoryName}")
        }
        b.tvAddress.text = r.addressText
        b.tvClient.text = r.clientName?.takeIf { it.isNotBlank() } ?: "—"
        b.tvDescription.text = r.description.takeIf { it.isNotBlank() } ?: "—"

        applyStatus(r.status)
        updateButtons(r)
    }

    private fun updateButtons(r: ServiceRequestResponse) {
        when (r.status) {
            "OPEN" -> {
                b.btnClaimWork.visibility = View.VISIBLE
                b.btnClaimWork.isClickable = true
                b.btnClaimWork.alpha = 1f
                b.btnClaimWork.setOnClickListener { claim(r) }
                b.btnCompleteRequest.visibility = View.GONE
            }
            "IN_PROGRESS" -> {
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

    private fun claim(r: ServiceRequestResponse) {
        b.btnClaimWork.isClickable = false
        b.btnClaimWork.alpha = 0.6f

        lifecycleScope.launch {
            runCatching { ApiClient.api.claimRequest(r.id) }
                .onSuccess { updated ->
                    request = updated
                    applyStatus(updated.status)
                    updateButtons(updated)
                    Toast.makeText(requireContext(), "Заявка взята в работу!", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    b.btnClaimWork.isClickable = true
                    b.btnClaimWork.alpha = 1f
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
            "OPEN"        -> Triple(getString(R.string.status_open_str), R.color.mk_status_open_bg, R.color.mk_status_open_fg)
            "IN_PROGRESS" -> Triple(getString(R.string.status_in_progress_str), R.color.mk_status_progress_bg, R.color.mk_status_progress_fg)
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
