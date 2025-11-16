package com.example.recipegroceryhelper

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class GroceryListsFragment : Fragment() {

    data class GroceryList(
        val id: String = "",
        val name: String = "",
        val timestamp: Long = 0
    )

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val groceryLists = mutableListOf<GroceryList>()
    private lateinit var adapter: GroceryListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_grocery_lists, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val groceryListName = view.findViewById<EditText>(R.id.NewListName)
        val addListButton = view.findViewById<Button>(R.id.AddList)
        val listView = view.findViewById<ListView>(R.id.ViewLists)

        adapter = GroceryListAdapter(requireContext(), groceryLists, ::onListClicked, ::onListDeleted)
        listView.adapter = adapter

        addListButton.setOnClickListener {
            val listName = groceryListName.text.toString().trim()
            if (listName.isNotEmpty()) {
                addNewList(listName)
                groceryListName.text.clear()
            }
        }

        loadGroceryLists()

        return view
    }

    private fun addNewList(listName: String) {
        val userId = auth.currentUser?.uid ?: return
        val listId = database.reference.push().key ?: return

        val groceryList = GroceryList(listId, listName)

        database.reference
            .child("users/$userId/groceryLists/$listId")
            .setValue(groceryList)
    }

    private fun loadGroceryLists() {
        val userId = auth.currentUser?.uid ?: return

        database.reference
            .child("users/$userId/groceryLists")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groceryLists.clear()
                    for (listSnapshot in snapshot.children) {
                        val list = listSnapshot.getValue(GroceryList::class.java)
                        if (list != null) {
                            groceryLists.add(list)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun onListClicked(groceryList: GroceryList) {
        val intent = Intent(requireContext(), GroceryItemsActivity::class.java)
        intent.putExtra("LIST_ID", groceryList.id)
        intent.putExtra("LIST_NAME", groceryList.name)
        startActivity(intent)
    }

    private fun onListDeleted(groceryList: GroceryList) {
        val userId = auth.currentUser?.uid ?: return

        database.reference
            .child("users/$userId/groceryLists/${groceryList.id}")
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "List deleted", Toast.LENGTH_SHORT).show()
            }
    }
}
