package com.example.recipegroceryhelper

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class GroceryItemsActivity : AppCompatActivity(), SensorEventListener {

    // data class to represent a grocery item
    data class GroceryItem(
        val id: String = "",
        val name: String = "",
        val imageUrl: String = ""
    )

    // firebase authentication, database, and storage references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    // adapter to display items in ListView
    private lateinit var itemAdapter: GroceryItemAdapter

    // list to store all grocery items from Firebase
    private val items = mutableListOf<GroceryItem>()

    // current grocery list information
    private var groceryListId: String = ""
    private var groceryListName: String = ""

    // URI for captured photo
    private var photoUri: Uri? = null

    // shake sensor variables
    private lateinit var sensorManager: SensorManager
    private var lastShakeTime = System.currentTimeMillis()
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    // request camera permission and launch camera if granted
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    // handle photo capture result
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { photoTaken ->
        // if photo was taken successfully, save it to Firebase
        if (photoTaken && photoUri != null) {
            saveItemWithPhoto()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grocery_items)

        // initialize Firebase services
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        // get grocery list id and grocery list name from previous grocery list activity
        groceryListId = intent.getStringExtra("LIST_ID") ?: ""
        groceryListName = intent.getStringExtra("LIST_NAME") ?: "Grocery List"

        // set up shake sensor using TYPE_ROTATION_VECTOR
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

        setupViews()
        getItems()
    }

    // initialize UI components
    // set up on click listeners
    private fun setupViews() {
        //grocery list name field
        val groceryListText = findViewById<TextView>(R.id.Title)

        // grocery item input field, add grocery item button, take photo button, and back button field
        val itemInput = findViewById<EditText>(R.id.NewItem)
        val addItemButton = findViewById<Button>(R.id.AddItem)
        val takePhotoButton = findViewById<Button>(R.id.TakeImage)
        val backButton = findViewById<Button>(R.id.Back)
        val itemsList = findViewById<ListView>(R.id.Items)

        // set the grocery list name as title
        groceryListText.text = groceryListName

        // set up the adapter to showcase the grocery items and to manage delete
        itemAdapter = GroceryItemAdapter(this, items, ::removeItem)
        itemsList.adapter = itemAdapter

        // add a new grocery item when the add button is clicked
        addItemButton.setOnClickListener {
            // get the text from the grocery item input field
            val itemText = itemInput.text.toString().trim()
            // if the text is not empty, save the grocery item to the firebase
            if (itemText.isNotEmpty()) {
                saveItem(itemText, "")
                //clear
                itemInput.text.clear()
            } else {
                Toast.makeText(this, "Enter item name", Toast.LENGTH_SHORT).show()
            }
        }

        // check for camera permission and open the camera when the take photo button is clicked
        takePhotoButton.setOnClickListener {
            checkPermissionOpenCamera()
        }

        // close activity and return to prev screen
        backButton.setOnClickListener {
            finish()
        }
    }

    // check if camera permission is granted, request permission if not
    private fun checkPermissionOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // permission already granted - open camera
                launchCamera()
            }
            else -> {
                // if permission not granted then request it
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // create temporary file and launch the camera to take photo
    private fun launchCamera() {
        // create a temporary file to store the photo
        val photoFile = File.createTempFile(
            // prefix for the file name
            "IMG_",
            // jpg suffix for the file name
            ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        )

        // get URI for the file using FileProvider
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)

        // launch the camera with the URI
        cameraLauncher.launch(photoUri!!)
    }

    // upload a photo to the firebase storage and save the item with photo URL
    private fun saveItemWithPhoto() {
        // get the current users id
        val userId = auth.currentUser?.uid ?: return

        // get the grocery item name from the grocery item input field
        val itemText = findViewById<EditText>(R.id.NewItem).text.toString().trim()

        // get the photo URI
        val imageUri = photoUri ?: return

        // upload the photo to firebase storage
        storage.reference.child("users/$userId/items/${userId}_item.jpg")
            .putFile(imageUri)
            .addOnSuccessListener {
                // once uploaded, get the download URL
                it.storage.downloadUrl.addOnSuccessListener { url ->
                    // save item with photo URL to database
                    saveItem(itemText, url.toString())
                    // clear grocery item input field
                    findViewById<EditText>(R.id.NewItem).text.clear()
                }
            }
    }

    // save a new grocery item to the firebase realtime database
    private fun saveItem(itemText: String, photoUrl: String) {
        // get the current user id
        val userId = auth.currentUser?.uid ?: return

        // generate unique id for the new grocery item
        val itemKey = database.reference.push().key ?: return

        // create a new groceryiitem object
        val newItem = GroceryItem(
            id = itemKey,
            name = itemText,
            imageUrl = photoUrl
        )

        // save item to Firebase under this grocery list
        database.reference
            .child("users/$userId/groceryLists/$groceryListId/items/$itemKey")
            .setValue(newItem)
    }

    // load all items for this grocery list from Firebase
    private fun getItems() {
        val userId = auth.currentUser?.uid ?: return

        // listen for changes to the items in this grocery list
        database.reference
            .child("users/$userId/groceryLists/$groceryListId/items")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(data: DataSnapshot) {
                    // clear current items list
                    items.clear()

                    // loop through each grocery item in Firebase and add to list
                    for (snapshot in data.children) {
                        val item = snapshot.getValue(GroceryItem::class.java)
                        if (item != null) {
                            items.add(item)
                        }
                    }

                    // update the ListView to show the new data
                    itemAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // delete an grocery item from firebase
    private fun removeItem(item: GroceryItem) {
        // get current user ID
        val userId = auth.currentUser?.uid ?: return

        // remove grocery item from firebase
        database.reference
            // reference to the item in the database
            .child("users/$userId/groceryLists/$groceryListId/items/${item.id}")
            .removeValue()
    }

    // shake sensor listener - using TYPE_ROTATION_VECTOR from lecture
    override fun onSensorChanged(event: SensorEvent) {
        // rotation about x, y, z axes
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShakeTime > 1000) {
            // detect shake by measuring rotation changes
            if (abs(lastX - x) > 0.5f || abs(lastY - y) > 0.5f || abs(lastZ - z) > 0.5f) {
                lastShakeTime = currentTime
                generateRecipe()
            }
        }

        // always update last values
        lastX = x
        lastY = y
        lastZ = z
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // not needed for this implementation
    }

    // generate recipe using all grocery items
    private fun generateRecipe() {
        if (items.isEmpty()) return
        Toast.makeText(this, "🔍 Finding recipe...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val url = URL("https://www.themealdb.com/api/json/v1/1/search.php?s=${items[0].name}")
                val conn = url.openConnection() as HttpURLConnection
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val meals = json.optJSONArray("meals") ?: return@Thread

                if (meals.length() > 0) {
                    var bestRecipe = meals.getJSONObject(0)
                    var bestMatchCount = 0

                    for (i in 0 until meals.length()) {
                        val meal = meals.getJSONObject(i)
                        val matchCount = items.count { groceryItem ->
                            (1..20).any { j ->
                                meal.optString("strIngredient$j", "").lowercase()
                                    .contains(groceryItem.name.lowercase())
                            }
                        }
                        if (matchCount > bestMatchCount) {
                            bestMatchCount = matchCount
                            bestRecipe = meal
                        }
                    }
                    showRecipe(bestRecipe)
                }
            } catch (e: Exception) {}
        }.start()
    }

    // show recipe detail
    private fun showRecipe(meal: JSONObject) {
        val name = meal.getString("strMeal")
        val inst = meal.getString("strInstructions")
        val imageUrl = meal.getString("strMealThumb")
        val ingredientsList = StringBuilder()
        for (i in 1..20) {
            val ingredient = meal.optString("strIngredient$i", "")
            val measure = meal.optString("strMeasure$i", "")
            if (ingredient.isNotEmpty()) {
                ingredientsList.append("• $ingredient ($measure)\n")
            }
        }
        runOnUiThread {
            val view = LayoutInflater.from(this).inflate(R.layout.recipe_detail, null)
            view.findViewById<TextView>(R.id.recipeName).text = "✨ $name"
            view.findViewById<TextView>(R.id.ingredients).text = ingredientsList.toString()
            view.findViewById<TextView>(R.id.instructions).text = inst
            view.findViewById<Button>(R.id.backButton).setOnClickListener { recreate() }
            Thread {
                try {
                    val conn = URL(imageUrl).openConnection() as HttpURLConnection
                    val bitmap = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    runOnUiThread { view.findViewById<ImageView>(R.id.recipeImage).setImageBitmap(bitmap) }
                } catch (e: Exception) {}
            }.start()
            setContentView(view)
        }
    }

    override fun onPause() {
        super.onPause()
        // unregister sensor listener when activity pauses
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        // register sensor listener when activity resumes
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
}