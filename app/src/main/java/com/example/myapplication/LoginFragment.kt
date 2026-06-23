package com.example.myapplication

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentLoginBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.LoginRequest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!
    private var passwordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentLoginBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.setDrawerEnabled(false)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.app_name))
        (activity as? MainActivity)?.setNotificationIconVisible(false)

        // Если уже авторизован — сразу на список задач
        if (AuthManager.isLoggedIn) {
            findNavController().navigate(R.id.action_login_to_taskList)
            return
        }

        b.ivTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            b.etPassword.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            b.etPassword.setSelection(b.etPassword.text?.length ?: 0)
            b.ivTogglePassword.alpha = if (passwordVisible) 1f else 0.5f
        }

        b.btnLogin.setOnClickListener { doLogin() }

        // Демо вход — заполняет поля и логинится
        b.btnDemo.setOnClickListener {
            b.etTenantCode.setText("esc-ural")
            b.etLogin.setText("kozlov_d")
            b.etPassword.setText("demo123")
            doLogin()
        }
    }

    private fun doLogin() {
        val tenantCode = b.etTenantCode.text?.toString()?.trim() ?: ""
        val username   = b.etLogin.text?.toString()?.trim() ?: ""
        val password   = b.etPassword.text?.toString()?.trim() ?: ""

        if (username.isEmpty() || password.isEmpty()) {
            shake(b.btnLogin)
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            runCatching {
                ApiClient.api.login(LoginRequest(tenantCode, username, password))
            }.onSuccess { response ->
                AuthManager.save(response)
                findNavController().navigate(R.id.action_login_to_taskList)
            }.onFailure { err ->
                setLoading(false)
                val msg = err.message ?: "Ошибка входа"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                shake(b.btnLogin)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        b.btnLogin.isClickable = !loading
        b.btnLogin.text = if (loading) "Вход…" else "ВОЙТИ"
        b.btnLogin.alpha = if (loading) 0.7f else 1f
    }

    private fun shake(v: View) {
        v.animate().translationX(12f).setDuration(60).withEndAction {
            v.animate().translationX(-12f).setDuration(60).withEndAction {
                v.animate().translationX(6f).setDuration(50).withEndAction {
                    v.animate().translationX(0f).setDuration(50).start()
                }.start()
            }.start()
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
