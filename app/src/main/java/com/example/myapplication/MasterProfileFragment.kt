package com.example.myapplication

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.myapplication.databinding.FragmentMasterProfileBinding
import com.example.myapplication.network.ApiClient
import com.example.myapplication.network.ServiceResponse
import com.example.myapplication.network.UpdateMasterProfileRequest
import com.example.myapplication.network.toUserMessage
import kotlinx.coroutines.launch

class MasterProfileFragment : Fragment() {

    private var _b: FragmentMasterProfileBinding? = null
    private val b get() = _b!!
    private val checkboxes = mutableListOf<CheckBox>()
    private var avatarUrl: String? = null
    private var profileLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentMasterProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.setDrawerEnabled(true)
        (activity as? MainActivity)?.setToolbarTitle(getString(R.string.master_profile))
        (activity as? MainActivity)?.setNotificationIconVisible(true)

        b.etSpecializationSearch.doAfterTextChanged { filterSpecializations() }
        b.btnSave.setOnClickListener { save() }

        profileLoaded = false
        b.btnSave.isEnabled = false
        load()
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val categoriesResult = runCatching { ApiClient.api.getCategories() }
            val servicesResult = runCatching { ApiClient.api.getServices() }
            val profileResult = runCatching { ApiClient.api.getMasterProfile() }

            if (categoriesResult.isFailure || servicesResult.isFailure || profileResult.isFailure) {
                val err = categoriesResult.exceptionOrNull() ?: servicesResult.exceptionOrNull() ?: profileResult.exceptionOrNull()
                Toast.makeText(requireContext(), "Ошибка загрузки: ${err?.toUserMessage()}", Toast.LENGTH_LONG).show()
                return@launch
            }

            val categories = categoriesResult.getOrThrow().flatMap { listOf(it) + it.subcategories.orEmpty() }.associateBy { it.id }
            val services = servicesResult.getOrThrow()
            val profile = profileResult.getOrThrow()
            avatarUrl = profile.avatarUrl

            b.etCity.setText(profile.city ?: "")
            b.etBio.setText(profile.bio ?: "")
            b.tvFullName.text = profile.fullName ?: AuthManager.fullName ?: ""
            b.tvRatingAvg.text = "★ ${String.format("%.1f", profile.ratingAvg)}"
            b.tvRatingCount.text = "${profile.ratingCount} ${reviewWord(profile.ratingCount)}"
            profile.avatarUrl?.let { url ->
                b.ivAvatar.load(AppSettings.mediaUrl(url)) {
                    crossfade(true)
                    placeholder(R.drawable.bg_input)
                }
            }

            buildSpecializationList(services, categories.mapValues { it.value.name }, profile.specializationIds.toSet())
            profileLoaded = true
            b.btnSave.isEnabled = true
            loadReviews(profile.userId)
        }
    }

    private fun reviewWord(n: Int) = when {
        n % 100 in 11..19 -> "отзывов"
        n % 10 == 1        -> "отзыв"
        n % 10 in 2..4     -> "отзыва"
        else                -> "отзывов"
    }

    private fun loadReviews(masterId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { ApiClient.api.getMasterReviews(masterId, pageSize = 20) }
                .onSuccess { page ->
                    b.llReviews.removeAllViews()
                    page.items.forEach { review ->
                        val card = TextView(requireContext()).apply {
                            text = "★ ${review.rating}" + (review.comment?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink))
                            setBackgroundResource(R.drawable.bg_card)
                            textSize = 13f
                            val dp = resources.displayMetrics.density
                            setPadding((16*dp).toInt(), (14*dp).toInt(), (16*dp).toInt(), (14*dp).toInt())
                            val lp = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            lp.bottomMargin = 8
                            layoutParams = lp
                        }
                        b.llReviews.addView(card)
                    }
                }
                .onFailure {
                    if (_b == null) return@onFailure /* reviews are supplementary -- profile still works without them */ }
        }
    }

    private fun buildSpecializationList(
        services: List<ServiceResponse>,
        categoryNames: Map<Long, String>,
        checkedIds: Set<Long>
    ) {
        b.llSpecializations.removeAllViews()
        checkboxes.clear()

        val byCategory = services.filter { it.active }.groupBy { it.categoryId }
        byCategory.forEach { (categoryId, servicesInCategory) ->
            val header = TextView(requireContext()).apply {
                text = categoryNames[categoryId] ?: "Категория"
                tag = "category"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink))
                textSize = 14f
                setPadding(0, 16, 0, 6)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            b.llSpecializations.addView(header)

            servicesInCategory.forEach { service ->
                val checkBox = CheckBox(requireContext()).apply {
                    text = service.name
                    isChecked = checkedIds.contains(service.id)
                    tag = service.id
                    buttonTintList = ContextCompat.getColorStateList(requireContext(), R.color.mk_accent)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink))
                }
                checkboxes.add(checkBox)
                b.llSpecializations.addView(checkBox)
            }
        }

        filterSpecializations()

        if (services.isEmpty()) {
            b.llSpecializations.addView(TextView(requireContext()).apply {
                text = "Каталог услуг пуст"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.mk_ink_hint))
                textSize = 13f
            })
        }
    }

    private fun filterSpecializations() {
        val query = b.etSpecializationSearch.text.toString().trim()
        // Checked services remain selected even when filtered out of view.
        checkboxes.forEach { it.visibility = if (query.isEmpty() || it.text.contains(query, ignoreCase = true)) View.VISIBLE else View.GONE }
        var header: View? = null
        var visible = false
        for (index in 0 until b.llSpecializations.childCount) {
            val child = b.llSpecializations.getChildAt(index)
            if (child.tag == "category") {
                header?.visibility = if (visible) View.VISIBLE else View.GONE
                header = child
                visible = false
            } else if (child.visibility == View.VISIBLE) visible = true
        }
        header?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun save() {
        if (!profileLoaded) return
        val specializationIds = checkboxes.filter { it.isChecked }.map { it.tag as Long }
        val request = UpdateMasterProfileRequest(
            avatarUrl = avatarUrl,
            city = b.etCity.text?.toString()?.trim() ?: "",
            bio = b.etBio.text?.toString()?.trim() ?: "",
            specializationIds = specializationIds
        )

        b.btnSave.isClickable = false
        b.btnSave.alpha = 0.6f

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { ApiClient.api.updateMasterProfile(request) }
                .onSuccess {
                    Toast.makeText(requireContext(), getString(R.string.master_profile_saved), Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    if (_b == null) return@onFailure
                    Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_LONG).show()
                }
            _b?.btnSave?.isClickable = true
            _b?.btnSave?.alpha = 1f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
