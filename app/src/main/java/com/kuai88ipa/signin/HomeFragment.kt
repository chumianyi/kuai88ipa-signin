package com.kuai88ipa.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: AppListAdapter
    private var currentCategory = "/"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        chipGroup = view.findViewById(R.id.chipGroup)

        adapter = AppListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadData(currentCategory) }

        // 分类Tab
        val categories = listOf(
            "推荐" to "/",
            "游戏" to "/game.html",
            "软件" to "/soft.html"
        )
        categories.forEachIndexed { index, (name, path) ->
            val chip = Chip(context).apply {
                text = name
                isCheckable = true
                id = View.generateViewId()
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        currentCategory = path
                        loadData(path)
                    }
                }
            }
            chipGroup.addView(chip)
            if (index == 0) chip.isChecked = true
        }

        loadData(currentCategory)
    }

    private fun loadData(path: String) {
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        tvEmpty.visibility = View.GONE

        ApiClient.getInstance(requireContext()).fetchAppList(path) { success, apps, error ->
            activity?.runOnUiThread {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                if (success) {
                    adapter.setData(apps)
                    if (apps.isEmpty()) tvEmpty.visibility = View.VISIBLE
                } else {
                    tvEmpty.text = error
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }
    }
}
