package com.airatlovesmusic.scanner.ui.documents.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.airatlovesmusic.scanner.R
import com.airatlovesmusic.scanner.entity.Document
import kotlinx.android.synthetic.main.item_document.view.*
import java.text.DateFormat
import java.util.*

/**
 * Created by Airat Khalilov on 17/09/2020.
 */

class DocumentHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    fun bind(data: Document) {
        itemView.iv_preview.setImageResource(R.drawable.ic_launcher_background)
        itemView.tv_name.text = data.id
        itemView.tv_created_at.text = DateFormat.getDateInstance().format(Date(data.createdAt))
    }
}