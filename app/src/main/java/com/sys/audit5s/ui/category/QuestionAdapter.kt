package com.sys.audit5s.ui.category

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sys.audit5s.R
import com.sys.audit5s.databinding.ItemQuestionBinding

class QuestionAdapter(
    private val onClick: (QuestionUiState) -> Unit
) : ListAdapter<QuestionUiState, QuestionAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(
        private val binding: ItemQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuestionUiState) {
            binding.tvQuestionNum.text = item.question.number.toString()
            binding.tvQuestionText.text = item.question.text
            binding.tvScore.text = item.score.toString()

            val colorRes = when (item.score) {
                0 -> R.color.score_0
                1 -> R.color.score_1
                2 -> R.color.score_2
                else -> R.color.score_3
            }
            binding.tvScore.backgroundTintList =
                ContextCompat.getColorStateList(binding.root.context, colorRes)

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<QuestionUiState>() {
        override fun areItemsTheSame(a: QuestionUiState, b: QuestionUiState) =
            a.question.number == b.question.number
        override fun areContentsTheSame(a: QuestionUiState, b: QuestionUiState) =
            a.score == b.score
    }
}