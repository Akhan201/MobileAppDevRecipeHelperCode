package com.example.recipegroceryhelper

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        // Load the default fragment when the activity starts
        loadFragment(RecipesFragment())

        // *** THIS IS THE CORRECTED PART ***
        // Set up the listener with a correctly structured 'when' block.
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Case for the Recipes tab
                R.id.navigation_recipes -> {
                    loadFragment(RecipesFragment())
                    true // Return true to indicate the event was handled
                }

                // Case for the Grocery List tab
                R.id.nav_grocery -> {
                    loadFragment(GroceryListsFragment())
                    true
                }

                R.id.nav_maps -> {
                    loadFragment( NearbyGroceryStoresMapsFragment())
                    true
                }

                // Default case for any other item
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
