package com.example.recipegroceryhelper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RecipesFragment : Fragment() {

    private lateinit var searchBar: EditText
    private lateinit var searchButton: Button
    private lateinit var recipeGrid: GridView
    private lateinit var detailView: View

    private val recipes = mutableListOf<Recipe>()

    data class Recipe(val id: String, val name: String, val imageUrl: String)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recipes, container, false)

        searchBar = view.findViewById(R.id.searchBar)
        searchButton = view.findViewById(R.id.searchButton)
        recipeGrid = view.findViewById(R.id.recipeList)

        val adapter = RecipeAdapter()
        recipeGrid.adapter = adapter

        searchButton.setOnClickListener {
            val query = searchBar.text.toString()
            if (query.isNotEmpty()) searchRecipes(query, adapter)
        }

        searchBar.setOnEditorActionListener { _, _, _ ->
            val query = searchBar.text.toString()
            if (query.isNotEmpty()) searchRecipes(query, adapter)
            true
        }

        recipeGrid.setOnItemClickListener { _, _, position, _ ->
            showRecipeDetails(recipes[position].id)
        }

        // load variety of recipes from multiple letters
        loadVariety(adapter)

        return view
    }

    private fun loadVariety(adapter: RecipeAdapter) {
        Thread {
            val all = mutableListOf<Recipe>()
            for (letter in listOf("b", "c", "s")) {
                try {
                    val url = URL("https://www.themealdb.com/api/json/v1/1/search.php?f=$letter")
                    val conn = url.openConnection() as HttpURLConnection
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    json.optJSONArray("meals")?.let { meals ->
                        for (i in 0 until meals.length()) {
                            val meal = meals.getJSONObject(i)
                            all.add(Recipe(
                                meal.getString("idMeal"),
                                meal.getString("strMeal"),
                                meal.getString("strMealThumb")
                            ))
                        }
                    }
                } catch (e: Exception) { }
            }
            activity?.runOnUiThread {
                recipes.clear()
                recipes.addAll(all)
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun searchRecipes(query: String, adapter: RecipeAdapter) {
        Thread {
            try {
                val url = URL("https://www.themealdb.com/api/json/v1/1/search.php?s=$query")
                val conn = url.openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().readText()

                val json = JSONObject(response)
                val meals = json.optJSONArray("meals")

                activity?.runOnUiThread {
                    recipes.clear()

                    if (meals != null) {
                        for (i in 0 until meals.length()) {
                            val meal = meals.getJSONObject(i)
                            recipes.add(Recipe(
                                meal.getString("idMeal"),
                                meal.getString("strMeal"),
                                meal.getString("strMealThumb")
                            ))
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), "No recipes found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error loading recipes", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showRecipeDetails(id: String) {
        Thread {
            try {
                val url = URL("https://www.themealdb.com/api/json/v1/1/lookup.php?i=$id")
                val conn = url.openConnection() as HttpURLConnection
                val response = conn.inputStream.bufferedReader().readText()

                val json = JSONObject(response)
                val meal = json.getJSONArray("meals").getJSONObject(0)

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

                activity?.runOnUiThread {
                    showDetailScreen(name, imageUrl, ingredientsList.toString(), inst)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Error loading details", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showDetailScreen(name: String, imageUrl: String, ingredientsList: String, inst: String) {
        val inflater = LayoutInflater.from(requireContext())
        detailView = inflater.inflate(R.layout.recipe_detail, null)

        val recipeName = detailView.findViewById<TextView>(R.id.recipeName)
        val recipeImage = detailView.findViewById<ImageView>(R.id.recipeImage)
        val ingredients = detailView.findViewById<TextView>(R.id.ingredients)
        val instructions = detailView.findViewById<TextView>(R.id.instructions)
        val backButton = detailView.findViewById<Button>(R.id.backButton)

        recipeName.text = name
        ingredients.text = ingredientsList
        instructions.text = inst

        loadImage(imageUrl, recipeImage)

        backButton.setOnClickListener {
            (view as ViewGroup).removeView(detailView)
            searchBar.visibility = View.VISIBLE
            searchButton.visibility = View.VISIBLE
            recipeGrid.visibility = View.VISIBLE
        }

        searchBar.visibility = View.GONE
        searchButton.visibility = View.GONE
        recipeGrid.visibility = View.GONE
        (view as ViewGroup).addView(detailView)
    }

    private fun loadImage(imageUrl: String, imageView: ImageView) {
        Thread {
            try {
                val url = URL(imageUrl)
                val conn = url.openConnection() as HttpURLConnection
                val bitmap = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                activity?.runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Image failed to load
            }
        }.start()
    }

    inner class RecipeAdapter : BaseAdapter() {
        override fun getCount() = recipes.size
        override fun getItem(position: Int) = recipes[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(requireContext())
                .inflate(R.layout.recipe_card, parent, false)

            val recipe = recipes[position]
            val nameView = view.findViewById<TextView>(R.id.recipe_name)
            val imageView = view.findViewById<ImageView>(R.id.recipe_image)

            nameView.text = recipe.name
            loadImage(recipe.imageUrl, imageView)

            return view
        }
    }
}