package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentMyRequestsBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class MyRequestsFragment : Fragment() {

    private var _b: FragmentMyRequestsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: ServiceRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentMyRequestsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.my_requests))
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        adapter = ServiceRequestAdapter(
            requests = mutableListOf(),
            showClaimButton = false,
            onClaim = {},
            onDetails = { request ->
                findNavController().navigate(
                    R.id.action_myRequests_to_requestDetail,
                    bundleOf("requestId" to request.id)
                )
            }
        )

        b.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        b.rvRequests.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }

    private fun loadRequests() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getMyRequests(pageSize = 100) }
                .onSuccess { page ->
                    adapter.replaceAll(page.items)
                    updateCount(page.items.size)
                    b.tvEmptyState.visibility = if (page.items.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure {
                    Toast.makeText(requireContext(), "Ошибка загрузки: ${it.toUserMessage()}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun updateCount(n: Int) {
        b.tvRequestCount.text = "$n ${requestWord(n)}"
    }

    private fun requestWord(n: Int) = when {
        n % 100 in 11..19 -> "заявок"
        n % 10 == 1        -> "заявка"
        n % 10 in 2..4     -> "заявки"
        else                -> "заявок"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
