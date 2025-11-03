package com.example.recipegroceryhelper
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

class GroceryListsActivity : AppCompatActivity() {

    data class GroceryList(
        val id: String = "",
        val name: String = ""
    )
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val listNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grocery_lists)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val etNewListName = findViewById<EditText>(R.id.NewListName)
        val btnAddList = findViewById<Button>(R.id.AddList)

        val listView = findViewById<ListView>(R.id.ViewLists)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listNames)
        listView.adapter = adapter

        btnAddList.setOnClickListener {
            val listName = etNewListName.text.toString()

            if (listName.isNotEmpty()) {
                addNewList(listName)
                etNewListName.text.clear()
            }
        }

        loadGroceryLists()
    }

    private fun addNewList(listName: String) {

        val userId = auth.currentUser?.uid ?: return
        val listId = database.reference.push().key ?: return

        val groceryList = GroceryList(id = listId, name = listName)

        database.reference
            .child("users")
            .child(userId)
            .child("groceryLists")
            .child(listId)
            .setValue(groceryList)
    }

    private fun loadGroceryLists() {
        val userId = auth.currentUser?.uid ?: return

        database.reference
            .child("users")
            .child(userId)
            .child("groceryLists")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listNames.clear()
                    for (listSnapshot in snapshot.children) {
                        val list = listSnapshot.getValue(GroceryList::class.java)
                        if (list != null) {
                            listNames.add(list.name)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}
