package com.kuai88ipa.signin

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AppListAdapter(
    private val apps: MutableList<AppInfo> = mutableListOf()
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    fun setData(newApps: List<AppInfo>) {
        apps.clear()
        apps.addAll(newApps)
        notifyDataSetChanged()
    }

    fun addData(newApps: List<AppInfo>) {
        val start = apps.size
        apps.addAll(newApps)
        notifyItemRangeInserted(start, newApps.size)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvInfo: TextView = itemView.findViewById(R.id.tvAppInfo)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val app = apps[pos]
                    val intent = Intent(itemView.context, DetailActivity::class.java).apply {
                        putExtra("app_id", app.id)
                        putExtra("app_name", app.name)
                    }
                    itemView.context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.name
        val info = buildString {
            if (app.version.isNotEmpty()) append("v${app.version}  ")
            if (app.size.isNotEmpty()) append(app.size)
            if (app.iosVersion.isNotEmpty()) append("  ·  ${app.iosVersion}")
        }
        holder.tvInfo.text = info

        Glide.with(holder.itemView.context)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_logo)
            .error(R.drawable.ic_logo)
            .into(holder.ivIcon)
    }

    override fun getItemCount() = apps.size
}
