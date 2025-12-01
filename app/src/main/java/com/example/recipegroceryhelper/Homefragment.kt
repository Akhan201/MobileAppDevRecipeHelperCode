package com.example.recipegroceryhelper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // set up click listeners for each card
        view.findViewById<CardView>(R.id.recipeCard).setOnClickListener {
            loadFragment(RecipesFragment())
        }

        view.findViewById<CardView>(R.id.groceryCard).setOnClickListener {
            loadFragment(GroceryListsFragment())
        }

        view.findViewById<CardView>(R.id.mapsCard).setOnClickListener {
            loadFragment(NearbyGroceryStoresMapsFragment())
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}