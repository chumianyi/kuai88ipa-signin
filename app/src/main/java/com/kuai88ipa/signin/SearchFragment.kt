package com.kuai88ipa.signin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var chipHistory: ChipGroup
    private lateinit var adapter: AppListAdapter
    private lateinit var prefs: android.content.SharedPreferences

    companion object {
        private const val HISTORY_PREFS = "search_history"
        private const val HISTORY_KEY = "keywords"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etSearch = view.findViewById(R.id.etSearch)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        chipHistory = view.findViewById(R.id.chipHistory)

        prefs = requireContext().getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)

        adapter = AppListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = etSearch.text.toString().trim()
                if (keyword.isNotEmpty()) {
                    saveHistory(keyword)
                    doSearch(keyword)
                }
                true
            }
            false
        }

        loadHistoryChips()
    }

    private fun doSearch(keyword: String) {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        recyclerView.visibility = View.GONE

        ApiClient.getInstance(requireContext()).searchApps(keyword) { success, apps, error ->
            activity?.runOnUiThread {
                progressBar.visibility = View.GONE
                if (success) {
                    if (apps.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        adapter.setData(apps)
                    } else {
                        tvEmpty.text = "未找到 \"$keyword\" 相关应用"
                        tvEmpty.visibility = View.VISIBLE
                    }
                } else {
                    tvEmpty.text = error
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }

        // 关闭键盘
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun saveHistory(keyword: String) {
        val history = getHistory().toMutableList()
        history.remove(keyword)
        history.add(0, keyword)
        if (history.size > 10) history.subList(10, history.size).clear()
        prefs.edit().putStringSet(HISTORY_KEY, history.toSet()).apply()
        loadHistoryChips()
    }

    private fun getHistory(): List<String> {
        return prefs.getStringSet(HISTORY_KEY, emptySet())?.toList() ?: emptyList()
    }

    private fun loadHistoryChips() {
        chipHistory.removeAllViews()
        val history = getHistory()
        if (history.isEmpty()) {
            chipHistory.visibility = View.GONE
            return
        }
        chipHistory.visibility = View.VISIBLE
        history.forEach { keyword ->
            val chip = Chip(context).apply {
                text = keyword
                setOnClickListener { doSearch(keyword) }
            }
            chipHistory.addView(chip)
        }
    }
}
