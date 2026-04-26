package com.sys.audit5s.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sys.audit5s.data.db.entity.AuditEntity
import com.sys.audit5s.databinding.ItemAuditBinding

class AuditAdapter(
    private val onClick: (AuditEntity) -> Unit
) : ListAdapter<AuditEntity, AuditAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemAuditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AuditEntity) {
            binding.tvSection.text = item.sectionName
            binding.tvAuditor.text = item.auditorName
            binding.tvDate.text = item.date
            binding.tvCategoryBadge.text = item.finalCategory ?: "—"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAuditBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<AuditEntity>() {
        override fun areItemsTheSame(a: AuditEntity, b: AuditEntity) = a.id == b.id
        override fun areContentsTheSame(a: AuditEntity, b: AuditEntity) = a == b
    }
}