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
import com.example.myapplication.databinding.FragmentMyActsBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

/** Read-only list of the inspector's completed tasks — tapping one opens the
 * corresponding act screen, which shows a locked, already-filled form once
 * the act exists (see NewActFragment/ActReplacementFragment "act exists" mode). */
class MyActsFragment : Fragment() {

    private var _b: FragmentMyActsBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: NetworkTaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentMyActsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle("Мои акты")
        (activity as? MainActivity)?.setNotificationIconVisible(false)

        adapter = NetworkTaskAdapter(
            tasks = mutableListOf(),
            onAccept = {}, // completed tasks never show the accept button
            onDetails = { task ->
                val dest = if (task.type == "REPLACEMENT") R.id.action_myActs_to_actReplacement else R.id.action_myActs_to_newAct
                findNavController().navigate(dest, bundleOf("taskId" to task.id))
            }
        )
        b.rvActs.layoutManager = LinearLayoutManager(requireContext())
        b.rvActs.adapter = adapter

        loadActs()
    }

    override fun onResume() {
        super.onResume()
        loadActs()
    }

    private fun loadActs() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getTasks(status = "COMPLETED", pageSize = 100) }
                .onSuccess { page ->
                    adapter.replaceAll(page.items)
                    b.tvActsCount.text = "${page.items.size} ${actWord(page.items.size)}"
                    b.tvEmpty.visibility = if (page.items.isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun actWord(n: Int) = when {
        n % 100 in 11..19 -> "актов"
        n % 10 == 1       -> "акт"
        n % 10 in 2..4    -> "акта"
        else              -> "актов"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
