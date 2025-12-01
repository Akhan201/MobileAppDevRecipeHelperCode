package com.example.recipegroceryhelper

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import androidx.core.app.ActivityCompat

class NearbyGroceryStoresMapsFragment : Fragment() {


    private lateinit var groceryMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_nearby_grocery_stores_maps, container, false)

        // initialize places SDK with the API key
        if (!Places.isInitialized()) {

            Places.initialize(requireContext(), getString(R.string.google_map_key))
        }
        // create PlacesClient instance that will be used to search for nearby places
        placesClient = Places.createClient(requireContext())

        // initialize FusedLocationProviderClient to get the user's current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        //get the Map Fragment from the map layout and request notification when the map is ready to be used
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // Wait for map to load
        mapFragment.getMapAsync { googleMap ->
            // Set the map to the groceryMap variable
            groceryMap = googleMap
            // Check for location permission
            checkLocationPermission()
        }
        return view
    }

    //
    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    //checks if the app has permisison to access the devices fine location
    // if permission granted, get the users current location, if not granted request permission
    private fun checkLocationPermission() {
        //check if we already have location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            //if location permission not granted, request it from the user
            ActivityCompat.requestPermissions(
                requireActivity(),
                //request fine locaiton permission
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            //if permission already granted, then proceed to get users location
            getCurrentLocation()
        }
    }

    // handles the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //check if this is the response to our location permission request
        if (requestCode == REQUEST_LOCATION_PERMISSION &&
            grantResults.isNotEmpty() && //makes sure result is not empty
            //check if permission was granted
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // user granted permission to access location, now can get their location
            getCurrentLocation()
        } else {
            // the user denied permission, show a message to the user informing that location permission required for this map feature
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // get last known location using the Fused Location Provider
    // last known location is cached location of the device and can be instantly retrieved
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        //enables the location layer on the map
        //blue dot indicating the users location
        groceryMap.isMyLocationEnabled = true

        //request the last known location from fushed location provider, returns cached location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            //if location is not null,create latlng object from latitude and longitude of users location
            if (location != null) {
                val userLocation = LatLng(location.latitude, location.longitude)

                // center and zoom the map camera to users location
                groceryMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                // add marker at the users location
                groceryMap.addMarker(
                    MarkerOptions()
                        .position(userLocation)
                        .title("Your Location")
                )

                //user location now obtained, now search for nearby grocery stores
                findNearbyGroceryStores(userLocation)
            } else {
                Toast.makeText(requireContext(), "Unable to get your location. Make sure your're GPS is enabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Searches for nearby grocery stores within 1.5km of user's location using google places API
    private fun findNearbyGroceryStores(location: LatLng) {
        //specifies which data fields we want API to return for each grocery store
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        //create circular search area within 1.5 km radius of users current location
        val circle = CircularBounds.newInstance(location, 1500.0)

        //create search request with the specified parameters(circular search area, and data fields)
        val searchRequest = SearchNearbyRequest.builder(circle, placeFields)
            //only return grocery stores
            .setIncludedTypes(listOf("grocery_store", "supermarket"))
            //limit results to 20 stores max
            .setMaxResultCount(20)
            .build()

        //search for places that match the search request
        placesClient.searchNearby(searchRequest).addOnCompleteListener { task ->
            val groceryStoreCount = task.result.places.size

            // Display message showing how many stores were found
            Toast.makeText(
                requireContext(),
                "Found $groceryStoreCount grocery stores nearby",
                Toast.LENGTH_SHORT
            ).show()
           //check if search was successful and results not null
            if (task.isSuccessful && task.result != null) {
                //loop through each grocery store in search results
                task.result.places.forEach { place ->
                    //only processes grocery stores that have valid lat/long
                    place.latLng?.let { position ->
                        //add a marker for each grocery store in the search results
                        val marker = MarkerOptions()
                            .position(position)
                            .title(place.name ?: "Grocery Store") //store name
                            .snippet(place.address) //address of store
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)) //make markers orange
                        //add marker to map
                        groceryMap.addMarker(marker)
                    }
                }
            }
        }
    }
}