/*
 * Copyright (c) 2018 GoMeta Inc. All Rights Reserver
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gometa.examples.objrenderdemo

import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.view_obj_preview.view.*
import java.io.File

/**
 *
 */
class ObjSelectorAdapter : RecyclerView.Adapter<ObjSelectorAdapter.ViewHolder>() {

    private var data: MutableList<Pair<String, Boolean>> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var selectedPosition = -1
        set(value) {
            if (field >= 0 && data[field].second) {
                data[field] = data[field].copy(second = false)
                notifyItemChanged(field)
            }
            if (value < 0) {
                field = -1
                onSelectedAssetChanged?.invoke(null)
                return
            }
            if (!data[value].second) {
                data[value] = data[value].copy(second = true)
                notifyItemChanged(value)
            }
            field = value
            onSelectedAssetChanged?.invoke(data[value].first)
        }

    var selectedItemBackgroundColor: Int = Color.WHITE
        private set
    var onSelectedAssetChanged: ((assetDirectory: String?)->Unit)? = null


    fun setAssetDirectories(directories: List<String>) {
        data = directories.map { it to false }.toMutableList()
        selectedPosition = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.view_obj_preview, parent, false))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position].first, data[position].second)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.unbind()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(assetDirectory: String, isSelected: Boolean) {
            val bitmap = BitmapFactory.decodeStream(
                itemView.context.assets.open("$assetDirectory${File.separator}icon.png"))
            itemView.icon.setImageBitmap(bitmap)
            itemView.icon.setOnClickListener {
                selectedItemBackgroundColor = bitmap.getPixel(0, 0)
                selectedPosition = adapterPosition
            }
            itemView.setBackgroundColor(if (isSelected) Color.LTGRAY else Color.TRANSPARENT)
        }

        fun unbind() {
            itemView.icon.setOnClickListener(null)
        }
    }
}