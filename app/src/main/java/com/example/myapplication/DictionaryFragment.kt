package com.example.myapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentDictionaryBinding
import com.example.myapplication.databinding.ItemAddressCardBinding
import com.example.myapplication.network.AddressResponse
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DictionaryFragment : Fragment() {

    private var _b: FragmentDictionaryBinding? = null
    private val b get() = _b!!
    private val allAddresses = mutableListOf<AddressResponse>()
    private lateinit var adapter: AddressAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentDictionaryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle("Справочник адресов")
        (activity as? MainActivity)?.setNotificationIconVisible(false)

        adapter = AddressAdapter(mutableListOf())
        b.rvAddresses.layoutManager = LinearLayoutManager(requireContext())
        b.rvAddresses.adapter = adapter

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // debounce
                    searchAddresses(s?.toString()?.trim() ?: "")
                }
            }
        })

        loadAddresses("")
    }

    private fun loadAddresses(query: String) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getAddresses(search = query, pageSize = 100) }
                .onSuccess { page ->
                    allAddresses.clear()
                    allAddresses.addAll(page.items)
                    adapter.replace(page.items)
                    updateCount(page.items.size)
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun searchAddresses(query: String) {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getAddresses(search = query, pageSize = 100) }
                .onSuccess { page ->
                    adapter.replace(page.items)
                    updateCount(page.items.size)
                }
        }
    }

    private fun updateCount(n: Int) {
        b.tvCount.text = "Найдено: $n адресов"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class AddressAdapter(
        private val items: MutableList<AddressResponse>
    ) : RecyclerView.Adapter<AddressAdapter.VH>() {

        inner class VH(private val b: ItemAddressCardBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(addr: AddressResponse) {
                val parts = listOfNotNull(
                    addr.street,
                    addr.house?.let { h -> buildString {
                        append("д. $h")
                        if (!addr.building.isNullOrBlank()) append(", корп. ${addr.building}")
                        if (!addr.apartment.isNullOrBlank()) append(", кв. ${addr.apartment}")
                    }}
                )
                b.tvAddressFull.text = parts.joinToString(", ")
                b.tvConsumer.text = addr.consumerName ?: "Потребитель не указан"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemAddressCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size

        fun replace(list: List<AddressResponse>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
    }
}
