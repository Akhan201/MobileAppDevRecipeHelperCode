package com.example.recipegroceryhelper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

class GroceryListAdapter(
    context: Context,
    private val lists: List<GroceryListsActivity.GroceryList>,
    private val onListClicked: (GroceryListsActivity.GroceryList) -> Unit,
    private val onListDeleted: (GroceryListsActivity.GroceryList) -> Unit
) : ArrayAdapter<GroceryListsActivity.GroceryList>(context, 0, lists) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val list = lists[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_grocery_list, parent, false)

        val tvListName = view.findViewById<TextView>(R.id.tvListName)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteList)

        // Set list name
        tvListName.text = list.name

        // Delete button listener
        btnDelete.setOnClickListener {
            onListDeleted(list)
        }

        // Click on list to open items
        view.setOnClickListener {
            onListClicked(list)
        }

        return view
    }
}