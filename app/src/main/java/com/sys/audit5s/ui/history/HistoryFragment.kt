package com.sys.audit5s.ui.history

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sys.audit5s.R
import com.sys.audit5s.databinding.FragmentHistoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: AuditAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        binding.toolbar.title = "История аудитов"
        setupRecyclerView()
        setupObservers()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.audits.collect { audits ->
                binding.toolbar.subtitle =
                    if (audits.isEmpty()) "Нет аудитов"
                    else "Проведено аудитов: ${audits.size}"
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = AuditAdapter { audit ->
            findNavController().navigate(
                R.id.summaryFragment,
                bundleOf("auditId" to audit.id)
            )
        }
        binding.rvAudits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAudits.adapter = adapter

        // Свайп влево для удаления
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val audit = adapter.currentList[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить аудит?")
                    .setMessage(
                        "Аудит \"${audit.sectionName}\" от ${audit.date} " +
                                "будет удалён безвозвратно вместе со всеми замечаниями и фото."
                    )
                    .setPositiveButton("Удалить") { _, _ ->
                        viewModel.deleteAudit(audit)
                    }
                    .setNegativeButton("Отмена") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()

                // Красный фон
                paint.color = Color.parseColor("#f44336")
                val background = RectF(
                    itemView.right + dX,
                    itemView.top.toFloat() + 8,
                    itemView.right.toFloat(),
                    itemView.bottom.toFloat() - 8
                )
                c.drawRoundRect(background, 16f, 16f, paint)

                // Текст "Удалить"
                paint.color = Color.WHITE
                paint.textSize = 36f
                paint.textAlign = Paint.Align.CENTER
                val textX = itemView.right - 100f
                val textY = itemView.top + (itemView.height / 2f) + 12f
                c.drawText("Удалить", textX, textY, paint)

                super.onChildDraw(
                    c, recyclerView, viewHolder,
                    dX, dY, actionState, isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvAudits)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.audits.collect { audits ->
                adapter.submitList(audits)

                if (audits.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.rvAudits.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.rvAudits.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}