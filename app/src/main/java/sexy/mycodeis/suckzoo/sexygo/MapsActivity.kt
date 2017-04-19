package sexy.mycodeis.suckzoo.sexygo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.util.Log
import com.github.kittinunf.fuel.core.Response
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import rx.Observable
import rx.Subscription
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MapsActivity : FragmentActivity(),
        OnMapReadyCallback,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener {

    lateinit var mService: KaistGoService

    private lateinit var mMarkerMap: MarkerMap

    private var mMap: GoogleMap? = null
    lateinit var mapsFragment: SupportMapFragment

    private var subscription: Subscription? = null
    lateinit var mGoogleApiClient: GoogleApiClient
    lateinit var mLocationRequest: LocationRequest
    lateinit var mLocationSettingsRequest: LocationSettingsRequest
    var mCurrentLocation: Location? = null

    private fun checkPermission(): Boolean {
        var flag = true
        val permissions = arrayOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")
        val hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, 1)
                flag = false
            }
        }
        return flag
    }


    fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_maps)
        checkPermission()

        mService = KaistGoService(resources.getString(R.string.base_url))
        mMarkerMap = MarkerMap(this)

        mapsFragment = map as SupportMapFragment
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapsFragment.getMapAsync(this)
        buildGoogleApiClient()
        createLocationRequest()
        buildLocationSettingsRequest()
    }

    fun subscribe() {
        subscription = Observable.interval(1, TimeUnit.SECONDS)
                .switchMap {
                    Log.wtf("firstSwitchMap", "emitted!")
                    mCurrentLocation?.let {
                        val latitude = it.latitude
                        val longitude = it.longitude
                        Log.wtf("firstSwitchMap", "$latitude, $longitude")
                        mService.search(latitude, longitude)
                    }
                }
                .map {
                    val response: Response = it.first
                    when (response.httpStatusCode) {
                        302 -> {
                            Log.wtf("http", "redirection")
                            val location = response.httpResponseHeaders["Location"]!![0]
                            mMarkerMap.addMarker(mCurrentLocation!!.latitude,
                                    mCurrentLocation!!.longitude,
                                    location)
                        }
                        else -> {
                            Log.wtf("http", "error..?" + response.httpStatusCode)
                        }
                    }
                }
                .retry()
                .subscribe {
                }
    }

    fun unsubscribe() {
        subscription?.let {
            Log.wtf("unsubscribe", "Disposing...")
            if (!it.isUnsubscribed) it.unsubscribe()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mGoogleApiClient.isConnected) {
            startLocationUpdates()
        }
        subscribe()
    }

    override fun onPause() {
        super.onPause()
        unsubscribe()
        if (mGoogleApiClient.isConnected) {
            stopLocationUpdates()
        }
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient.connect()
    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient.isConnected) {
            mGoogleApiClient.disconnect()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.wtf("onMapReady", "map ready")
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        // val sydney = LatLng(-34.0, 151.0)
        // mMap!!.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap!!.moveCamera(CameraUpdateFactory.zoomBy(5F))
        mMap!!.isMyLocationEnabled = true
        mMap!!.setOnMarkerClickListener(this)
    }

    fun addMarker(lat: Double, lng: Double, title: String) {
        Log.wtf("addMarker", "adding marker to $lat, $lng")
        val coord = LatLng(lat, lng)
        mMap!!.addMarker(MarkerOptions().position(coord).title(title))
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        Log.wtf("onMarkerClick", "marker clicked")
        marker?.let {
            Log.wtf("onMarkerClick", "marker is not null")
            val url = it.title
            mMarkerMap.fetchPayloadFromMarker(url)
                    .map {
                        Log.wtf("onMarkerClick", "fetched payload")

                        var intent = Intent(this, VisualizeActivity::class.java)

                        val contentType = it.first
                        var payload = it.second
                        intent.putExtra("contentType", contentType)
                        if (contentType != "video/mp4") {
                            intent.putExtra("payload", payload)
                        } else {
                            val file = File(this.filesDir, "sexy_video.mp4")
                            val path = file.path
                            val out = FileOutputStream(file)
                            intent.putExtra("filePath", path)
                            out.write(payload)
                            out.close()
                        }
                        startActivity(intent)
                    }
                    .subscribe {

                    }
            return true
        }
        return false
    }

    /**
     * Google Play Location Service fail/suspension handler
     */
    fun startLocationUpdates() {
        LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient,
                mLocationSettingsRequest
        ).setResultCallback({
            when (it.status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> {
                    Log.i("locationUpdates", "All location settings are satisfied")
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this
                    )
                }
            }
        })
    }

    fun stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient,
                this
        ).setResultCallback {

        }
    }

    override fun onConnected(connectionHint: Bundle?) {
        Log.i("onConnected", "connected")
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        }
        startLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
        Log.wtf("onLocationChaged", "location changed")
        mCurrentLocation = location
        mCurrentLocation?.let {
            val locLatLng: LatLng = LatLng(it.latitude, it.longitude)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(locLatLng, 15F)
            mMap!!.animateCamera(cameraUpdate)
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.i("onConnectionFailed", "Connection failed: error code " + result.errorCode)
    }

    override fun onConnectionSuspended(cause: Int) {
        Log.i("onConnectionSuspended", "Connection suspended")
        mGoogleApiClient.connect()
    }
}
