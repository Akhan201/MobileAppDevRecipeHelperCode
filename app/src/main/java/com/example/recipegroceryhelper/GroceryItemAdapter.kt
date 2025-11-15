package com.example.recipegroceryhelper

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class GroceryItemAdapter(
    context: Context,
    private val itemsList: List<GroceryItemsActivity.GroceryItem>,
    private val onCheckChange: (GroceryItemsActivity.GroceryItem, Boolean) -> Unit,
    private val onDelete: (GroceryItemsActivity.GroceryItem) -> Unit
) : ArrayAdapter<GroceryItemsActivity.GroceryItem>(context, 0, itemsList) {

    override fun getView(position: Int, oldView: View?, parent: ViewGroup): View {
        val currentItem = itemsList[position]
        val itemView = oldView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_grocery, parent, false)

        val nameText = itemView.findViewById<TextView>(R.id.tvItemName)
        val deleteBtn = itemView.findViewById<ImageButton>(R.id.btnDeleteItem)
        val photoView = itemView.findViewById<ImageView>(R.id.ivItemImage)

        nameText.text = currentItem.name

        // Show photo if exists
        if (currentItem.imageUrl.isNotEmpty()) {
            photoView.visibility = View.VISIBLE
            Glide.with(context)
                .load(currentItem.imageUrl)
                .centerCrop()
                .into(photoView)
        } else {
            photoView.visibility = View.GONE
        }



        deleteBtn.setOnClickListener {
            onDelete(currentItem)
        }

        return itemView
    }
}