package com.masai.uber.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.directions.route.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import com.masai.uber.R
import com.masai.uber.databinding.ActivityStartTripBinding
import com.masai.uber.remote.NetworkRetrofit
import com.masai.uber.utlis.*
import com.masai.uber_rider.remote.ApiClient
import com.masai.uber_rider.remote.models.LegsItem
import com.masai.uber_rider.remote.models.Result
import com.masai.uber_rider.remote.models.RoutesItem
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.layout_confirm_trip.*
import kotlinx.android.synthetic.main.layout_confirm_trip.view.*
import kotlinx.android.synthetic.main.layout_start_trip.*
import kotlinx.android.synthetic.main.layout_start_trip.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Response
import java.util.ArrayList


class StartTripActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.OnConnectionFailedListener,
    RoutingListener {

    private lateinit var newPos: LatLng
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityStartTripBinding

    //polyline object
    private var polylines: ArrayList<Polyline>? = null

    private var selectPlaceEvent: TripEvent? = null

    //Routes
    private var apiClient: ApiClient? = null
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineList: ArrayList<LatLng?>? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    //Location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var startAddress: String
    private lateinit var endAddress: String
    private lateinit var dDistance: String
    private lateinit var dDuration: String

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectPlaceEvent(event: TripEvent) {
        selectPlaceEvent = event
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStartTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent != null && intent.extras != null) {
            startAddress = intent.getStringExtra(START_ADDRESS).toString()
            endAddress = intent.getStringExtra(END_ADDRESS).toString()
        }

        initViews()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapTrip) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onStart() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        startLocationUpdate()
        super.onStart()
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    override fun onStop() {
        if (!EventBus.getDefault().hasSubscriberForEvent(TripEvent::class.java)) {
            EventBus.getDefault().removeStickyEvent(TripEvent::class.java)
            EventBus.getDefault().unregister(this)
        }
        stopLocationUpdate()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initViews() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 500
        locationRequest.smallestDisplacement = 10f
        locationRequest.interval = 500

        locationCallback = object : LocationCallback() {
            //ctrl+o
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                newPos = LatLng(p0.lastLocation.latitude, p0.lastLocation.longitude)
            }
        }

        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient
            .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)

        apiClient = NetworkRetrofit.getInstanceRe()?.create(ApiClient::class.java)
        press_ok.btnPressOk.setOnClickListener {
            findRoutes(
                newPos,
                selectPlaceEvent?.destination!!
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(6.0f)
        mMap.setMaxZoomPreference(14.0f)

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                selectPlaceEvent?.origin, 18f
            )
        )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.animateCamera(CameraUpdateFactory.zoomIn())
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10f), 2000, null)

        val cameraPosition = CameraPosition.Builder()
            .target(selectPlaceEvent?.destination) // Sets the center of the map to Mountain View
            .zoom(17f)            // Sets the zoom
            .bearing(90f)         // Sets the orientation of the camera to east
            .tilt(30f)            // Sets the tilt of the camera to 30 degrees
            .build()              // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        val locationButton = (mapFragment.requireView()!!
            .findViewById<View>("1".toInt())!!.parent!! as View)
            .findViewById<View>("2".toInt())

        val params = locationButton.layoutParams as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        params.bottomMargin = 250

        try {
            val success = googleMap!!.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_maps_style)
            )
            if (!success) {

            }
        } catch (e: Exception) {

        }

        drawPath2(
            selectPlaceEvent?.origin!!, selectPlaceEvent?.destination!!
        )
    }

    private fun startLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient
            .requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)
    }

    private fun stopLocationUpdate() {
        fusedLocationProviderClient
            .removeLocationUpdates(locationCallback)
    }

    private fun drawPath2(origin: LatLng, destination: LatLng) {
        apiClient?.getDirections(
            startAddress,
            endAddress,
            getString(R.string.google_maps_key)
        )?.enqueue(object : retrofit2.Callback<Result> {

            override fun onResponse(call: Call<Result?>, response: Response<Result?>) {
                if (response.isSuccessful) {
                    val result: Result? = response.body()
                    val routes: List<RoutesItem?>? = result?.routes
                    val legs: List<LegsItem?>? = routes?.get(0)?.legs
                    dDistance = legs?.get(0)?.distance?.text.toString()
                    dDuration = legs?.get(0)?.duration?.text.toString()
                    val ssAddress = legs?.get(0)?.startAddress.toString()
                    val eeAddress = legs?.get(0)?.endAddress.toString()
                    val polyline = legs?.get(0)?.steps?.get(0)?.polyline.toString()
                    polylineList = Common.decodePoly(polyline)
                    Log.d("tag", dDistance)
                    Log.d("tag", dDuration)

//                    findRoutes(origin, destination)

                    EventBus.getDefault().postSticky(
                        TripEvent(
                            LatLngConvertor.getOrigin(startAddress),
                            LatLngConvertor.getOrigin(endAddress),
                            ssAddress, eeAddress, dDistance, dDuration
                        )
                    )
                }

            }

            override fun onFailure(call: Call<Result?>, t: Throwable) {
                Log.d("tag", "Exception")
            }

        })
    }

    private fun findRoutes(origin: LatLng, destination: LatLng) {
        val routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(origin, destination)
            .key("AIzaSyBrgPQhpR6cePo-zHYSxNfEwQY6MqNI74w") //also define your api key here.
            .build()
        routing.execute()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        findRoutes(
            newPos,
            selectPlaceEvent?.destination!!
        )
    }

    override fun onRoutingFailure(p0: RouteException?) {
        findRoutes(
            newPos,
            selectPlaceEvent?.destination!!
        )
    }

    override fun onRoutingStart() {
    }

    override fun onRoutingSuccess(p0: ArrayList<Route>?, p1: Int) {
        val center = CameraUpdateFactory.newLatLng(newPos)
        val zoom = CameraUpdateFactory.zoomTo(18f)
        polylines?.clear()
        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        var polylineEndLatLng: LatLng? = null
        polylines = ArrayList()
        //add route(s) to the map using polyline
        for (i in 0 until p0!!.size) {
            if (i == p1) {
                polyOptions.color(Color.GRAY)
                polyOptions.width(10f)
                polyOptions.addAll(p0.get(p1).getPoints())
                val polyline = mMap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                (polylines as ArrayList<Polyline>).add(polyline)
            } else {
            }
        }


        /** val startMarker = MarkerOptions()
        startMarker.position(polylineStartLatLng!!)
        startMarker.title("My Location")
        mMap.addMarker(startMarker) **/

        //Add Marker on route ending position
        //Add Marker on route ending position
        /**val view1 = layoutInflater.inflate(R.layout.destination_infor_window, null)
        val tvDestinaftion = view1.findViewById<View>(R.id.tvDestinationMap) as TextView
        tvDestinaftion.text=eeAddress

        val generator1 = IconGenerator(this)
        generator.setContentView(view1)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        val icon1 = generator1.makeIcon()

        destinationMarker = mMap.addMarker(
        MarkerOptions()
        .icon(BitmapDescriptorFactory.fromBitmap(icon1))
        .position(selectPlaceEvent!!.destination)
        ) **/

        val startMarker = MarkerOptions()
        startMarker.position(polylineStartLatLng!!)
        startMarker.title("Origin")
        startMarker.icon(
            getBitmapDescriptorFromVector(
                this,
                R.drawable.car
            )
        )
        mMap.addMarker(startMarker)

        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title("Destination")
        endMarker.icon(
            getBitmapDescriptorFromVector(
                this,
                R.drawable.ic__375372_logo_uber_icon__1_
            )
        )
        mMap.addMarker(endMarker)

        press_ok.visibility = View.GONE
        confirm_trip.visibility = View.VISIBLE
        confirm_trip.btnConfirmUberABC.setOnClickListener {
            startActivity(Intent(this, StartJourneyActivity::class.java))
            finish()
        }

    }


    fun getBitmapDescriptorFromVector(
        context: Context,
        vectorDrawableResourceId: Int
    ): BitmapDescriptor? {

        val vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onRoutingCancelled() {
        findRoutes(
            newPos,
            selectPlaceEvent?.destination!!
        )
    }
}