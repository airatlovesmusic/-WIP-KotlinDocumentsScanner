package com.airatlovesmusic.scanner.ui.documents.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Document

/**
 * Created by Airat Khalilov on 17/09/2020.
 */

class DocumentsAdapter: RecyclerView.Adapter<DocumentHolder>() {

    private var list: List<Document> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return DocumentHolder(itemView)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(
        holder: DocumentHolder,
        position: Int
    ) = holder.bind(list[position])

    fun updateList(newList: List<Document>) {
        this.list = newList
        notifyDataSetChanged()
    }

}