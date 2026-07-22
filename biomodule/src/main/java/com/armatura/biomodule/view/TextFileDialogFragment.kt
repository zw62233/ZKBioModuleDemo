package com.armatura.biomodule.view

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.armatura.biomodule.R
import com.armatura.biomodule.databinding.DialogTextFileBinding

class LogAdapter : RecyclerView.Adapter<LogAdapter.VH>() {

    private var items: List<String> = emptyList()

    fun submit(list: List<String>) {
        this.items = list
        notifyDataSetChanged()
    }

    class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val tv = TextView(parent.context).apply {
            setPadding(8, 4, 8, 4)
        }
        return VH(tv)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
    }
}


class TextFileDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(): TextFileDialogFragment = TextFileDialogFragment()
    }

    lateinit var binding: DialogTextFileBinding
    private val adapter = LogAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.dialog_text_file,
            null,
            false
        )

        binding.rvContent.adapter = adapter
        binding.rvContent.layoutManager = LinearLayoutManager(requireContext())

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .create()
    }

    fun setContent(content: String) {
        binding.pbLoading.visibility = View.GONE
        binding.rvContent.visibility = View.VISIBLE

        // 关键：按行切割，不阻塞 UI
        val lines = content.split("\n")
        adapter.submit(lines)
    }
}

