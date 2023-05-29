package com.example.currentlocation

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.currentlocation.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


private const val PERMISSION_REQUEST_ACCESS_LOCATION = 1

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                checkGPS()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                checkGPS()
            }
            else -> {
                // No location access granted.
                requestPermission()
            }
        }
    }

    private val googleApiAvailability by lazy {
        GoogleApiAvailability.getInstance()
    }

    private val googleConnectionStatus by lazy {
        googleApiAvailability.isGooglePlayServicesAvailable(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        //Initialize fusedLocationProviderClient
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        binding.getLocationBtn.setOnClickListener {
            when {
                isGooglePlayServicesAvailable() -> fetchLocation()
                else -> Snackbar.make(
                    binding.root,
                    "Please Install Google Play Store.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            currentDateAndTime()
        }
    }

    //check google play services is exist or not
    private fun isGooglePlayServicesAvailable() = googleConnectionStatus == ConnectionResult.SUCCESS

    //fetching location
    private fun fetchLocation() {
        when {
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED -> {
                //When permission is denied
                requestPermission()
                return
            }
            else -> {
                //When Permission is allowed
                checkGPS()
            }
        }
    }

    //request permission for precise and approximate accuracy
    private fun requestPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    //checking GPS if it is off then request dialog to the user for enable the gps
    private fun checkGPS() {
        // Request high-accuracy locations
        val minIntervalMillis = 2000L
        val maxDelayMillis = 5000L
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, minIntervalMillis).apply {
                setMinUpdateIntervalMillis(minIntervalMillis)
                setMaxUpdateDelayMillis(maxDelayMillis)
                setWaitForAccurateLocation(false)
            }.build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val task = LocationServices.getSettingsClient(this.applicationContext)
            .checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            getCurrentLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        PERMISSION_REQUEST_ACCESS_LOCATION
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            when { // Last known location available
                location != null -> updateUI(location)
                else -> {  // No last known location, request new location updates
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest,
                        object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                super.onLocationResult(locationResult)
                                fusedLocationProviderClient.removeLocationUpdates(this)
                                val newLocation = locationResult.lastLocation
                                when {
                                    newLocation != null -> updateUI(newLocation)
                                }
                            }
                        },
                        Looper.getMainLooper()
                    )
                }
            }
        }
            .addOnFailureListener {
                // Failed to retrieve location
                Snackbar.make(
                    binding.root,
                    "Unable to retrieve the location please try again.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUI(location: Location) {
        binding.apply {
            longitudeTv.text = location.longitude.toString()
            latitudeTv.text = location.latitude.toString()
        }
    }

    //fetching current Date and Time
    private fun currentDateAndTime() {
        //checks the current Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            val formattedDateTime = currentDateTime.format(formatter)
            binding.dateTv.text = formattedDateTime
        } else {
            binding.dateTv.text = Date().toString()
        }
    }

}
