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
import com.example.myapplication.databinding.FragmentTaskListBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.TaskStatusUpdateRequest
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private var _b: FragmentTaskListBinding? = null
    private val b get() = _b!!
    private lateinit var adapter: NetworkTaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentTaskListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.my_tasks))
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        adapter = NetworkTaskAdapter(
            tasks = mutableListOf(),
            onAccept = { task -> acceptTask(task) },
            onDetails = { task ->
                findNavController().navigate(
                    R.id.action_taskList_to_orderDetail,
                    bundleOf("taskId" to task.id)
                )
            }
        )

        b.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        b.rvTasks.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            runCatching { ApiClient.api.getTasks() }
                .onSuccess { tasks ->
                    adapter.replaceAll(tasks)
                    updateCount(tasks.size)
                }
                .onFailure {
                    Toast.makeText(requireContext(), "Ошибка загрузки: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun acceptTask(task: com.example.myapplication.network.TaskResponse) {
        lifecycleScope.launch {
            runCatching {
                ApiClient.api.updateTaskStatus(task.id, TaskStatusUpdateRequest("IN_PROGRESS"))
            }.onSuccess { updated ->
                adapter.updateItem(updated)
                Toast.makeText(requireContext(), "Задача принята в работу", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCount(n: Int) {
        b.tvTaskCount.text = "$n ${taskWord(n)}"
    }

    private fun taskWord(n: Int) = when {
        n % 100 in 11..19 -> "задач"
        n % 10 == 1       -> "задача"
        n % 10 in 2..4    -> "задачи"
        else              -> "задач"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
