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
    private val lists: List<GroceryListsFragment.GroceryList>,
    private val onListClicked: (GroceryListsFragment.GroceryList) -> Unit,
    private val onListDeleted: (GroceryListsFragment.GroceryList) -> Unit
) : ArrayAdapter<GroceryListsFragment.GroceryList>(context, 0, lists) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val list = lists[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_grocery_list, parent, false)

        val groceryListName = view.findViewById<TextView>(R.id.listName)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteList)

        groceryListName.text = list.name

        deleteButton.setOnClickListener {
            onListDeleted(list)
        }

        view.setOnClickListener {
            onListClicked(list)
        }

        return view
    }
}