package com.example.recipegroceryhelper

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place

class NearbyGroceryStoresMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    // Lecture 7 Slide 25: Modern permission request using ActivityResultContracts
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Finding nearby stores...", Toast.LENGTH_SHORT).show()
            getCurrentLocationAndFindStores()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_grocery_stores_maps)

        // Lecture 7 Slide 40: Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_map_key))
        }
        placesClient = Places.createClient(this)

        // Lecture 7 Slide 18: Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Lecture 7 Slide 40: Get the SupportMapFragment and register callback
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Lecture 7 Slide 42: onMapReady callback - called when map is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    // Lecture 7 Slide 25: Check and request location permission
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocationAndFindStores()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Lecture 7 Slide 21: Get last known location using FusedLocationProviderClient
    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndFindStores() {
        mMap.isMyLocationEnabled = true

        // Lecture 7 Slide 21: lastLocation returns cached location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)

                // Lecture 7 Slide 44: Center map camera at user's location with zoom level
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                // Lecture 7 Slide 43: Add marker at user location with custom color
                mMap.addMarker(
                    MarkerOptions()
                        .position(userLocation)
                        .title("You are here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                findNearbyGroceryStores(userLocation)
            } else {
                // Handle case when GPS location is null
                Toast.makeText(this, "Unable to get location. Enable GPS and try again.", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            // Handle location fetch failure
            Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
        }
    }

    // Use Places API to search for nearby grocery stores
    private fun findNearbyGroceryStores(location: LatLng) {
        // Define what place data we want to retrieve
        val placeFields = listOf(
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS  // Get store address to display in marker
        )

        // Create circular search area with 1.5km radius
        val circle = CircularBounds.newInstance(location, 1500.0)

        // Build search request for grocery stores only
        val searchRequest = SearchNearbyRequest.builder(circle, placeFields)
            .setIncludedTypes(listOf("grocery_store", "supermarket"))
            .setMaxResultCount(20)
            .build()

        // Execute search
        placesClient.searchNearby(searchRequest).addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val response = task.result

                // Show toast with number of stores found
                Toast.makeText(
                    this,
                    "Found ${response.places.size} grocery stores nearby",
                    Toast.LENGTH_SHORT
                ).show()

                // Lecture 7 Slide 43: Add marker for each grocery store found
                for (place in response.places) {
                    if (place.latLng != null) {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(place.latLng!!)
                                .title(place.name ?: "Grocery Store")
                                .snippet(place.address ?: "Tap for details")  // Show address
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_GREEN
                                ))
                        )
                    }
                }
            } else {
                // Handle search failure
                Toast.makeText(this, "Failed to find stores", Toast.LENGTH_SHORT).show()
            }
        }
    }
}