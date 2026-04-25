package com.sys.audit5s.ui.summary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sys.audit5s.databinding.ItemRemarkBinding

class RemarkAdapter(
    private val onClick: (RemarkUiItem) -> Unit
) : ListAdapter<RemarkUiItem, RemarkAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemRemarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RemarkUiItem) {
            binding.tvRemarkCategory.text =
                "Категория ${item.category} · Вопрос ${item.questionNumber}"
            binding.tvRemarkDescription.text = item.remark.description
            binding.tvRemarkDeadline.text = "Срок: ${item.remark.deadlineDate}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRemarkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<RemarkUiItem>() {
        override fun areItemsTheSame(a: RemarkUiItem, b: RemarkUiItem) =
            a.remark.id == b.remark.id
        override fun areContentsTheSame(a: RemarkUiItem, b: RemarkUiItem) =
            a == b
    }
}