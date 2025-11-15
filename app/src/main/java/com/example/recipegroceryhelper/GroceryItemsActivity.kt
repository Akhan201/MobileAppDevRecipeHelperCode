package com.example.recipegroceryhelper

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class GroceryItemsActivity : AppCompatActivity() {

    data class GroceryItem(
        val id: String = "",
        val name: String = "",
        val imageUrl: String = ""
    )

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var itemAdapter: GroceryItemAdapter
    private val items = mutableListOf<GroceryItem>()
    private var groceryListId: String = ""
    private var groceryListName: String = ""
    private var photoUri: Uri? = null


    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { photoTaken ->
        if (photoTaken && photoUri != null) {
            saveItemWithPhoto()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grocery_items)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        groceryListId = intent.getStringExtra("LIST_ID") ?: ""
        groceryListName = intent.getStringExtra("LIST_NAME") ?: "Grocery List"


        setupViews()
        getItems()
    }

    private fun setupViews() {
        val groceryItemText = findViewById<TextView>(R.id.Title)
        val itemInput = findViewById<EditText>(R.id.NewItem)
        val addItemButton = findViewById<Button>(R.id.AddItem)
        val takePhotoButton = findViewById<Button>(R.id.TakeImage)
        val backButton = findViewById<Button>(R.id.Back)
        val itemsList = findViewById<ListView>(R.id.Items)

        groceryItemText.text = groceryListName

        itemAdapter = GroceryItemAdapter(this, items, ::removeItem)
        itemsList.adapter = itemAdapter

        addItemButton.setOnClickListener {
            val itemText = itemInput.text.toString().trim()
            if (itemText.isNotEmpty()) {
                saveItem(itemText, "")
                itemInput.text.clear()
            } else {
                Toast.makeText(this, "Enter item name", Toast.LENGTH_SHORT).show()
            }
        }

        takePhotoButton.setOnClickListener {
            launchCamera()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "IMG_",
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
        cameraLauncher.launch(photoUri!!)
    }

    private fun saveItemWithPhoto() {
        val userId = auth.currentUser?.uid ?: return
        val groceryItemText = findViewById<EditText>(R.id.NewItem).text.toString().trim()
        val imageUri = photoUri ?: return

        val imageName = "item_${System.currentTimeMillis()}.jpg"
        val imageRef = storage.reference
            .child("users/$userId/items/$imageName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { url ->
                    saveItem(groceryItemText, url.toString())
                    findViewById<EditText>(R.id.NewItem).text.clear()
                    Toast.makeText(this, "Item saved", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun saveItem(itemText: String, photoUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val itemKey = database.reference.push().key ?: return

        val newItem = GroceryItem(
            id = itemKey,
            name = itemText,
            imageUrl = photoUrl
        )

        database.reference
            .child("users/$userId/groceryLists/$groceryListId/items/$itemKey")
            .setValue(newItem)

    }

    private fun getItems() {
        val userId = auth.currentUser?.uid ?: return

        database.reference
            .child("users/$userId/groceryLists/$groceryListId/items")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(data: DataSnapshot) {
                    items.clear()
                    for (snapshot in data.children) {
                        val item = snapshot.getValue(GroceryItem::class.java)
                        if (item != null) {
                            items.add(item)
                        }
                    }

                    itemAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun removeItem(item: GroceryItem) {
        val userId = auth.currentUser?.uid ?: return
        database.reference
            .child("users/$userId/groceryLists/$groceryListId/items/${item.id}")
            .removeValue()
    }
}