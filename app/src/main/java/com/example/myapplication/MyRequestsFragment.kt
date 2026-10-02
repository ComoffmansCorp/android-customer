package com.example.myapplication

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
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
import com.example.myapplication.network.ServiceRequestResponse
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class MyRequestsFragment : Fragment() {

    private var _b: FragmentMyRequestsBinding? = null
    private val b get() = _b!!
    private var loadedRequests = emptyList<ServiceRequestResponse>()
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
        b.etSearch.doAfterTextChanged { filterRequests() }
        b.btnRefresh.setOnClickListener { loadRequests() }
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }

    private var loadingRequests = false

    private fun loadRequests() {
        if (loadingRequests) return
        loadingRequests = true
        b.progress.visibility = View.VISIBLE
        b.btnRefresh.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val first = ApiClient.api.getMyRequests(pageSize = 100)
                val all = first.items.toMutableList()
                for (page in 2..first.totalPages) all.addAll(ApiClient.api.getMyRequests(page = page, pageSize = 100).items)
                loadedRequests = all.distinctBy { it.id }.sortedByDescending { it.createdAt }
                filterRequests()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                b.tvRequestCount.text = "Заявки недоступны"
                b.tvEmptyState.text = "Не удалось загрузить заявки. Нажмите кнопку обновления."
                b.tvEmptyState.visibility = if (loadedRequests.isEmpty()) View.VISIBLE else View.GONE
                Toast.makeText(requireContext(), error.toUserMessage(), Toast.LENGTH_LONG).show()
            } finally {
                loadingRequests = false
                _b?.progress?.visibility = View.GONE
                _b?.btnRefresh?.isEnabled = true
            }
        }
    }

    private fun filterRequests() {
        val query = b.etSearch.text.toString().trim()
        val visible = loadedRequests.filter { query.isEmpty() || "${it.serviceName} ${it.addressText} ${it.description}".contains(query, ignoreCase = true) }
        adapter.replaceAll(visible)
        updateCount(visible.size)
        b.tvEmptyState.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        b.tvEmptyState.text = if (query.isNotEmpty()) "По этому запросу ничего не найдено. Попробуйте другие слова." else getString(R.string.empty_my_requests)
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
