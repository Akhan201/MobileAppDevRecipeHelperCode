package com.example.recipegroceryhelper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroceryListsActivity : AppCompatActivity() {

    data class GroceryList(
        val id: String = "",
        val name: String = "",
        val timestamp: Long = 0
    )

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val groceryLists = mutableListOf<GroceryList>()
    private lateinit var adapter: GroceryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grocery_lists)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val etNewListName = findViewById<EditText>(R.id.NewListName)
        val btnAddList = findViewById<Button>(R.id.AddList)
        val listView = findViewById<ListView>(R.id.ViewLists)

        // Setup custom adapter with delete functionality
        adapter = GroceryListAdapter(
            this,
            groceryLists,
            ::onListClicked,
            ::onListDeleted
        )
        listView.adapter = adapter

        // Add new list button
        btnAddList.setOnClickListener {
            val listName = etNewListName.text.toString().trim()

            if (listName.isNotEmpty()) {
                addNewList(listName)
                etNewListName.text.clear()
            } else {
                Toast.makeText(this, "Please enter a list name", Toast.LENGTH_SHORT).show()
            }
        }

        loadGroceryLists()
    }

    private fun addNewList(listName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val listId = database.reference.push().key ?: return
        val groceryList = GroceryList(
            id = listId,
            name = listName,
            timestamp = System.currentTimeMillis()
        )

        database.reference
            .child("users")
            .child(userId)
            .child("groceryLists")
            .child(listId)
            .setValue(groceryList)
            .addOnSuccessListener {
                Toast.makeText(this, "List added: $listName", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add list", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadGroceryLists() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        database.reference
            .child("users")
            .child(userId)
            .child("groceryLists")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    groceryLists.clear()
                    for (listSnapshot in snapshot.children) {
                        val list = listSnapshot.getValue(GroceryList::class.java)
                        if (list != null) {
                            groceryLists.add(list)
                        }
                    }

                    // Sort by timestamp (newest first)
                    groceryLists.sortByDescending { it.timestamp }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@GroceryListsActivity,
                        "Failed to load lists: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun onListClicked(groceryList: GroceryList) {
        val intent = Intent(this, GroceryItemsActivity::class.java)
        intent.putExtra("LIST_ID", groceryList.id)
        intent.putExtra("LIST_NAME", groceryList.name)
        startActivity(intent)
    }

    private fun onListDeleted(groceryList: GroceryList) {
        val userId = auth.currentUser?.uid ?: return

        // Show confirmation dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete List")
            .setMessage("Are you sure you want to delete '${groceryList.name}'? This will also delete all items in the list.")
            .setPositiveButton("Delete") { _, _ ->
                database.reference
                    .child("users")
                    .child(userId)
                    .child("groceryLists")
                    .child(groceryList.id)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "List deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete list", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}