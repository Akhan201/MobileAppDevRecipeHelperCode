package com.example.recipegroceryhelper

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.maps.android.SphericalUtil
import com.google.android.libraries.places.api.model.Place


class NearbyGroceryStoresMaps : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    // Modern way to request a permission.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission was granted. Continue the action.
            getCurrentLocationAndFindStores()
        } else {
            // Permission was denied. Log an error.
            Log.e("Location", "Location permission not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_grocery_stores_maps)

        // Initialize the Places SDK with the correct method name and closing parenthesis.
        // Make sure you have a 'google_maps_key' string resource in your strings.xml.
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)

        // Initialize the location client.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get the map fragment and trigger the onMapReady callback.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * This function is a required callback from OnMapReadyCallback.
     * It is called when the map is ready to be used.
     * It must be outside of onCreate.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    /**
     * This function checks if the app has location permission.
     * It must be outside of onCreate.
     */
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted.
                getCurrentLocationAndFindStores()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Explain to the user why you need the permission, then request it.
                // For simplicity, we'll just request it directly here.
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                // Directly request the permission.
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * Gets the user's current location and starts the search for stores.
     * Suppressing the warning because we know we call this only after checking for permission.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndFindStores() {
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                findNearbyGroceryStores(userLocation)
            } else {
                Log.e("MapsActivity", "Last location is null. Make sure location is enabled on the device.")
            }
        }
    }

    /**
     * Uses the Places API to find nearby grocery stores.
     */
    @SuppressLint("MissingPermission")
    private fun findNearbyGroceryStores(location: LatLng) {
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES)

        val locationRestriction = RectangularBounds.newInstance(
            SphericalUtil.computeOffset(location, 1500.0, 225.0),
            SphericalUtil.computeOffset(location, 1500.0, 45.0)
        )


        val searchRequest = SearchNearbyRequest.builder(
            locationRestriction,
            placeFields,
        ).build()





        val placeResult = placesClient.searchNearby(searchRequest)
        placeResult.addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val response = task.result
                for (place in response.places) {
                    mMap.addMarker(
                        MarkerOptions()
                            .position(place.latLng!!).title(place.name)
                    )
                    Log.i("MapsActivity", "Found grocery store: ${place.name}")
                }
            } else {
                Log.e("MapsActivity", "Exception while finding places: ", task.exception)
            }
        }
    }
}


