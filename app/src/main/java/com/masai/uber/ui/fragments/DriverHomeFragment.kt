package com.masai.uber.ui.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.directions.route.*
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.masai.uber.R
import com.masai.uber.databinding.FragmentDriverHomeBinding
import com.masai.uber.model.eventbus.DriverRequestReceived
import com.masai.uber.remote.IGoogleApi
import com.masai.uber.remote.NetworkRetrofit
import com.masai.uber.remote.RetrofitClient
import com.masai.uber.ui.activities.DriverHomeActivity
import com.masai.uber.ui.activities.StartTripActivity
import com.masai.uber.utlis.*
import com.masai.uber_rider.remote.ApiClient
import com.masai.uber_rider.remote.models.LegsItem
import com.masai.uber_rider.remote.models.Result
import com.masai.uber_rider.remote.models.RoutesItem
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_driver_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class DriverHomeFragment : Fragment(), OnMapReadyCallback,
    GoogleApiClient.OnConnectionFailedListener,
    RoutingListener {
    private var polylineStartLatLng: LatLng? = null
    private var polylineEndLatLng: LatLng? = null
    private var binding: FragmentDriverHomeBinding? = null

    //location
    private var mMap: GoogleMap? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: com.google.android.gms.location.LocationCallback
    private lateinit var mLocationReq: LocationRequest

    //firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var onlineRef: DatabaseReference
    private var currentUserRef: DatabaseReference? = null
    private lateinit var driverLocationRef: DatabaseReference
    private var geoFire: GeoFire? = null

    private lateinit var userId: String

    //Routes
    private val compositeDisposable = CompositeDisposable()
    private var iGoogleApi: IGoogleApi? = null
    private var apiClient: ApiClient? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineList: ArrayList<LatLng?>? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null

    //polyline object
    private var polylines: ArrayList<Polyline>? = null

    private var selectPlaceEvent: DriverRequestReceived? = null

    private lateinit var eAddress: String
    private var sAddress = "Hanuman Mandir Mattan Sidh 177001"
    private lateinit var eeAddress: String
    private lateinit var ssAddress: String
    private lateinit var dDistance: String
    private lateinit var dDuration: String
    private lateinit var origin: String
    private lateinit var destination: String
    private lateinit var startAddress: String
    private lateinit var endAddress: String

    private lateinit var mapFragment: SupportMapFragment

    private val valueEventListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentUserRef != null) {
                currentUserRef!!.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            AestheticDialog.Builder(
                requireActivity(),
                DialogStyle.TOASTER,
                DialogType.ERROR
            )
                .setTitle("Error ")
                .setMessage(error.message.toString())
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDriverHomeBinding.inflate(layoutInflater, container, false)

        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {

        apiClient = NetworkRetrofit.getInstanceRe()!!.create(ApiClient::class.java)
        iGoogleApi = RetrofitClient.getInstance()!!.create(IGoogleApi::class.java)
        /** Firebase and GeoFire **/
        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid.toString()
        onlineRef = FirebaseDatabase.getInstance().reference
            .child(".info/connected")
//        driverLocationRef =
//            FirebaseDatabase.getInstance().getReference(KEY_DRIVER_LOCATION_REFERENCE)
//
//                    currentUserRef = FirebaseDatabase.getInstance().getReference(KEY_DRIVER_LOCATION_REFERENCE)
//                        .child(userId)
//        geoFire = GeoFire(driverLocationRef)

//        registerOnlineSystem()

        /** Location **/
        mLocationReq = LocationRequest()
        mLocationReq.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationReq.smallestDisplacement = 50f //50m
        mLocationReq.interval = 15000 //15 sec
        mLocationReq.fastestInterval = 10000 //10 sec

        mLocationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val newPosition = LatLng(
                    locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
                )
                mMap?.moveCamera(
                    CameraUpdateFactory
                        .newLatLngZoom(newPosition, 18f)
                )
                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                val addressList: List<Address>?

                try {
                    addressList = geoCoder.getFromLocation(
                        locationResult.lastLocation.latitude,
                        locationResult.lastLocation.longitude, 1
                    )
                    val cityName = addressList[0].locality
                    if (cityName != null) {
                        driverLocationRef =
                            FirebaseDatabase.getInstance()
                                .getReference(KEY_DRIVER_LOCATION_REFERENCE)
                                .child(cityName)
                        currentUserRef = driverLocationRef.child(
                            FirebaseAuth.getInstance()
                                .currentUser!!.uid
                        )
                    } else {
                        driverLocationRef =
                            FirebaseDatabase.getInstance()
                                .getReference(KEY_DRIVER_LOCATION_REFERENCE)
                                .child("Hamirpur")
                        currentUserRef = driverLocationRef.child(
                            FirebaseAuth.getInstance()
                                .currentUser!!.uid
                        )
                    }
                    geoFire = GeoFire(driverLocationRef)
                    //update location
                    geoFire?.setLocation(
                        userId, GeoLocation(
                            locationResult.lastLocation.latitude,
                            locationResult.lastLocation.longitude
                        )
                    ) { key, error ->
                        if (error != null) {
                            AestheticDialog.Builder(
                                requireActivity(),
                                DialogStyle.TOASTER,
                                DialogType.ERROR
                            )
                                .setTitle("Error ")
                                .setMessage(error.message.toString())
                                .show()
                        } else {
//                        AestheticDialog.Builder(
//                            requireActivity(),
//                            DialogStyle.TOASTER,
//                            DialogType.SUCCESS
//                        )
//                            .setTitle("Success ")
//                            .setMessage("You are online")
//                            .show()
                        }
                    }
                    registerOnlineSystem()
                } catch (e: IOException) {
                    Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()

                }

//                //update location
//                geoFire?.setLocation(
//                    userId, GeoLocation(
//                        locationResult.lastLocation.latitude,
//                        locationResult.lastLocation.longitude
//                    )
//                ) { key, error ->
//                    if (error != null) {
//                        AestheticDialog.Builder(
//                            requireActivity(),
//                            DialogStyle.TOASTER,
//                            DialogType.ERROR
//                        )
//                            .setTitle("Error ")
//                            .setMessage(error.message.toString())
//                            .show()
//                    } else {
////                        AestheticDialog.Builder(
////                            requireActivity(),
////                            DialogStyle.TOASTER,
////                            DialogType.SUCCESS
////                        )
////                            .setTitle("Success ")
////                            .setMessage("You are online")
////                            .show()
//                    }
//                }
            }

        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        mFusedLocationClient.requestLocationUpdates(
            mLocationReq,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.mMap = googleMap
        Dexter.withContext(context)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }


                    mMap!!.isMyLocationEnabled = true
                    mMap!!.uiSettings.isMyLocationButtonEnabled = true
                    mMap!!.setOnMyLocationButtonClickListener {
                        mFusedLocationClient.lastLocation
                            .addOnFailureListener { e ->
                                Snackbar.make(
                                    requireView(), e.message!!,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            .addOnSuccessListener { location ->
                                val userLating = LatLng(location.latitude, location.longitude)
                                mMap!!.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        userLating,
                                        10f
                                    )
                                )

                            }

                        true
                    }

                    /**mMap!!.setOnMyLocationButtonClickListener(
                    object : GoogleMap.OnMyLocationButtonClickListener {
                    override fun onMyLocationButtonClick(): Boolean {
                    if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    ) {
                    return false
                    }
                    mFusedLocationClient.lastLocation
                    .addOnFailureListener {
                    //                                        AestheticDialog.Builder(
                    //                                            requireActivity(),
                    //                                            DialogStyle.TOASTER,
                    //                                            DialogType.SUCCESS
                    //                                        )
                    //                                            .setTitle("${it.message}")
                    //                                            .show()
                    }
                    .addOnSuccessListener {
                    val userLatLng = LatLng(
                    it.latitude,
                    it.longitude
                    )
                    mCurrentLocation = it
                    googleMap.animateCamera(
                    CameraUpdateFactory
                    .newLatLngZoom(userLatLng, 18f)
                    )
                    }
                    return true
                    }
                    })**/

                    //set location button
                    /** val locationButton =
                    (mapFragment.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(
                    Integer.parseInt("2")
                    )**/

                    val locationButton = (mapFragment.requireView()!!
                        .findViewById<View>("1".toInt())!!.parent!! as View)
                        .findViewById<View>("2".toInt())

                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 50

                    /** val rlp = locationButton.layoutParams as RelativeLayout.LayoutParams
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    rlp.setMargins(0, 0, 0, 50)**/
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    AestheticDialog.Builder(
                        requireActivity(),
                        DialogStyle.TOASTER,
                        DialogType.WARNING
                    )
                        .setTitle("Warning !")
                        .setMessage("You have denied the location permission")
                        .show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }

            }).onSameThread()
            .check()

        try {
            val success = mMap!!.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.uber_maps_style
                )
            )
            if (!success) {
                Log.d("tag", "error")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("tag", "error")
        }

    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(requireContext())
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    Toast.makeText(
                        requireContext(), "Go to Settings",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(valueEventListener)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }


    override fun onDestroy() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        geoFire?.removeLocation(FirebaseAuth.getInstance().currentUser?.uid)
        onlineRef.removeEventListener(valueEventListener)
        compositeDisposable.clear()

        if (EventBus.getDefault().hasSubscriberForEvent(DriverRequestReceived::class.java))
            EventBus.getDefault().removeStickyEvent(DriverRequestReceived::class.java)
        EventBus.getDefault().unregister(this)

        super.onDestroy()
        binding = null
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public fun onDriverRequestReceive(event: DriverRequestReceived) {
        selectPlaceEvent = event
        startAddress = event.startAddress.toString()
        endAddress = event.endAddress.toString()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        mFusedLocationClient.lastLocation
            .addOnFailureListener {
                it.message?.let { it1 ->
                    Snackbar.make(
                        requireView(),
                        it1, Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnSuccessListener {

                apiClient?.getDirections(
                    StringBuilder()
                        .append(it.latitude)
                        .append(",")
                        .append(it.longitude)
                        .toString(), event.pickUpLocation,
                    "AIzaSyBrgPQhpR6cePo-zHYSxNfEwQY6MqNI74w"
                )?.enqueue(object : retrofit2.Callback<Result> {
                    override fun onResponse(
                        call: Call<Result>,
                        response: Response<Result>
                    ) {
                        if (response.isSuccessful) {
                            val result: Result? = response.body()
                            val routes: List<RoutesItem?>? = result?.routes
                            val legs: List<LegsItem?>? = routes?.get(0)?.legs
                            ssAddress = legs?.get(0)?.startAddress.toString()
                            eeAddress = legs?.get(0)?.endAddress.toString()
                            dDistance = legs?.get(0)?.distance?.text.toString()
                            dDuration = legs?.get(0)?.duration?.text.toString()
                            val polyline = legs?.get(0)?.steps?.get(0)?.polyline.toString()
                            polylineList = Common.decodePoly(polyline)

                            Log.d("tag", ssAddress)
                            Log.d("tag", eeAddress)
                            Log.d("tag", dDistance)
                            Log.d("tag", dDuration)

                            origin = StringBuilder()
                                .append(it.latitude)
                                .append(",")
                                .append(it.longitude)
                                .toString()
                            destination = event.pickUpLocation!!

//                            EventBus.getDefault().postSticky(
//                                TripEvent(
//                                    LatLngConvertor.getOrigin(origin),
//                                    LatLngConvertor.getDestination(destination)
//                                )
//                            )


                            findRoutes(origin, destination)

                            /**  polylineOptions = PolylineOptions()
                            polylineOptions!!.color(Color.GRAY)
                            polylineOptions!!.width(12f)
                            polylineOptions!!.jointType(JointType.ROUND)
                            polylineOptions!!.startCap(SquareCap())
                            polylineOptions!!.addAll(polylineList!!)
                            greyPolyline = mMap?.addPolyline(polylineOptions!!)

                            blackPolylineOptions = PolylineOptions()
                            blackPolylineOptions!!.color(Color.BLACK)
                            blackPolylineOptions!!.width(12f)
                            blackPolylineOptions!!.jointType(JointType.ROUND)
                            blackPolylineOptions!!.startCap(SquareCap())
                            blackPolylineOptions!!.addAll(polylineList!!)
                            blackPolyline = mMap?.addPolyline(blackPolylineOptions!!)

                            //Animator
                            val valueAnimator = ValueAnimator.ofInt(0, 100)
                            valueAnimator.duration = 1100
                            valueAnimator.repeatCount = ValueAnimator.INFINITE
                            valueAnimator.interpolator = LinearInterpolator()
                            valueAnimator.addUpdateListener { value ->
                            val points = greyPolyline!!.points
                            val percentageValue = value.animatedValue.toString().toInt()
                            val size = points.size
                            val newPoints = (size * (percentageValue / 100.0f)).toInt()
                            val p = points.subList(0, newPoints)
                            blackPolyline!!.points = p
                            }
                            valueAnimator.start() **/

                            /**  val origin =
                            LatLng(it.latitude, it.longitude)

                            val destination =
                            LatLng(
                            event?.pickUpLocation!!.split(",")[0].toDouble(),
                            event?.pickUpLocation!!.split(",")[1].toDouble()
                            )

                            val latLngBound =
                            LatLngBounds.Builder().include(origin)
                            .include(destination).build()

                            //add car icon for origin

                            tvEstimateTime.text = dDistance.toString()
                            tvEstimatedDistance.text = dDistance.toString()

                            mMap?.addMarker(
                            MarkerOptions().position(destination)
                            .icon(BitmapDescriptorFactory.defaultMarker())
                            .title("Pickup Location")
                            )

                            mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                            mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.cameraPosition!!.zoom - 1))
                            //Display Layouts
                            chipIgnore.visibility = View.VISIBLE
                            layoutAccept.visibility = View.VISIBLE

                            //countdown
                            Observable.interval(100, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext {
                            circularProgressBar.progress += 1f
                            }
                            .takeUntil { aLong -> aLong == "100".toLong() }
                            .doOnComplete {
                            Toast.makeText(
                            context,
                            "Fake accept action ",
                            Toast.LENGTH_SHORT
                            ).show()
                            }.subscribe() **/

                        }
                    }

                    override fun onFailure(call: Call<Result>, t: Throwable) {
                        Log.d("tag", "Exception")
                    }

                })


                /**    iGoogleApi ?. getDirections (
                "driving",
                "less_driving",
                StringBuilder()
                .append(it.latitude)
                .append(",")
                .append(it.longitude)
                .toString(),
                event.pickUpLocation, getString(R.string.google_api_key)
                )
                ?.subscribeOn(io.reactivex.schedulers.Schedulers.io())
                ?.observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                ?.subscribe { result ->

                try {
                val jsonObj = JSONObject(result!!)
                val jsonArray = jsonObj.getJSONArray("routes")

                for (i in 0 until jsonArray.length()) {
                val route = jsonArray.getJSONObject(i)
                val poly = route.getJSONObject("overview_polyline")
                val polyline = poly.getString("points")
                polylineList = Common.decodePoly(polyline)
                }
                polylineOptions = PolylineOptions()
                polylineOptions!!.color(Color.GRAY)
                polylineOptions!!.width(12f)
                polylineOptions!!.jointType(JointType.ROUND)
                polylineOptions!!.startCap(SquareCap())
                polylineOptions!!.addAll(polylineList!!)
                greyPolyline = mMap?.addPolyline(polylineOptions!!)

                blackPolylineOptions = PolylineOptions()
                blackPolylineOptions!!.color(Color.BLACK)
                blackPolylineOptions!!.width(12f)
                blackPolylineOptions!!.jointType(JointType.ROUND)
                blackPolylineOptions!!.startCap(SquareCap())
                blackPolylineOptions!!.addAll(polylineList!!)
                blackPolyline = mMap?.addPolyline(blackPolylineOptions!!)

                //Animator
                val valueAnimator = ValueAnimator.ofInt(0, 100)
                valueAnimator.duration = 1100
                valueAnimator.repeatCount = ValueAnimator.INFINITE
                valueAnimator.interpolator = LinearInterpolator()
                valueAnimator.addUpdateListener { value ->
                val points = greyPolyline!!.points
                val percentageValue = value.animatedValue.toString().toInt()
                val size = points.size
                val newPoints = (size * (percentageValue / 100.0f)).toInt()
                val p = points.subList(0, newPoints)
                blackPolyline!!.points = p
                }
                valueAnimator.start()

                var origin =
                LatLng(it.latitude, it.longitude)

                var destination =
                LatLng(
                event?.pickUpLocation!!.split(",")[0].toDouble(),
                event?.pickUpLocation!!.split(",")[1].toDouble()
                )

                val latLngBound =
                LatLngBounds.Builder().include(origin)
                .include(destination).build()

                //add car icon for origin

                val objects = jsonArray.getJSONObject(0)
                val legs = objects.getJSONArray("legs")
                val legsObject = legs.getJSONObject(0)

                val time = legsObject.getJSONObject("duration")
                val duration = time.getString("text")

                val distanceEstimator = legsObject.getJSONObject("distance")
                val distance = distanceEstimator.getString("text")

                tvEstimateTime.text = duration.toString()
                tvEstimatedDistance.text = distance.toString()

                mMap?.addMarker(
                MarkerOptions().position(destination)
                .icon(BitmapDescriptorFactory.defaultMarker())
                .title("Pickup Location")
                )

                mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
                mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.cameraPosition!!.zoom - 1))


                //Display Layouts
                chipIgnore.visibility = View.VISIBLE
                layoutAccept.visibility = View.VISIBLE

                //countdown
                Observable.interval(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                circularProgressBar.progress += 1f
                }
                .takeUntil { aLong -> aLong == "100".toLong() }
                .doOnComplete {
                Toast.makeText(
                context,
                "Fake accept action ",
                Toast.LENGTH_SHORT
                ).show()
                }.subscribe()


                } catch (e: Exception) {
                e.printStackTrace()
                }
                }?.let {
                compositeDisposable?.add(
                it
                )
                } **/

            }
    }

    private fun findRoutes(origin: String, destination: String) {
        /** val latlngOrigin = origin.split(",".toRegex()).toTypedArray()
        val latlngDestination = destination.split(",".toRegex()).toTypedArray()

        val latitudeOrigin = latlngOrigin[0].toDouble()
        val longitudeOrigin = latlngOrigin[1].toDouble()
        val latlngOrigin:LatLng= LatLng(latitudeOrigin,longitudeOrigin)
        val latitudeDestination = latlngDestination[0].toDouble()
        val longitudeDestination = latlngDestination[1].toDouble()**/

        val routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(
                LatLngConvertor.getOrigin(origin),
                LatLngConvertor.getDestination(destination)
            )
            .key("AIzaSyBrgPQhpR6cePo-zHYSxNfEwQY6MqNI74w") //also define your api key here.
            .build()
        routing.execute()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        findRoutes(origin, destination)
    }

    override fun onRoutingFailure(p0: RouteException?) {
        findRoutes(origin, destination)
    }

    override fun onRoutingStart() {
    }

    override fun onRoutingSuccess(p0: java.util.ArrayList<Route>?, p1: Int) {
        val center = CameraUpdateFactory.newLatLng(LatLngConvertor.getOrigin(origin))
        val zoom = CameraUpdateFactory.zoomTo(16f)
        polylines?.clear()
        val polyOptions = PolylineOptions()
        polylines = ArrayList()
        //add route(s) to the map using polyline
        for (i in 0 until p0!!.size) {
            if (i == p1) {
                polyOptions.color(Color.BLUE)
                polyOptions.width(12f)
                polyOptions.addAll(p0.get(p1).getPoints())
                val polyline = mMap?.addPolyline(polyOptions)
                if (polyline != null) {
                    polylineStartLatLng = polyline.points[0]
                }
                val k = polyline?.points?.size
                if (polyline != null) {
                    if (k != null) {
                        polylineEndLatLng = polyline.points[k - 1]
                    }
                }
                if (polyline != null) {
                    (polylines as ArrayList<Polyline>).add(polyline)
                }
            } else {
            }
        }

//        val view = layoutInflater.inflate(R.layout.origin_infor_window, null)
//
//        val generator = IconGenerator(requireContext())
//        generator.setContentView(view)
//        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
//        val icon = generator.makeIcon()

//        originMarker = mMap?.addMarker(
//            MarkerOptions()
//                .icon(BitmapDescriptorFactory.fromBitmap(icon))
//                .position(LatLngConvertor.getOrigin(origin))
//        )

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
                requireContext(),
                R.drawable.ic__375372_logo_uber_icon__1_
            )
        )
        mMap?.addMarker(startMarker)

        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title("Destination")
        endMarker.icon(
            getBitmapDescriptorFromVector(
                requireContext(),
                R.drawable.ic_flag
            )
        )
        mMap?.addMarker(endMarker)

        val origin =
            LatLngConvertor.getOrigin(origin)

        val destination =
            LatLngConvertor.getDestination(destination)

        val latLngBound =
            LatLngBounds.Builder().include(origin)
                .include(destination).build()

        //add car icon for origin
        tvEstimateTime.text = dDuration.toString()
        tvEstimatedDistance.text = dDistance.toString()

        /** mMap?.addMarker(
        MarkerOptions().position(destination)
        .icon(BitmapDescriptorFactory.defaultMarker())
        .title("Pickup Location")
        )**/
        EventBus.getDefault().postSticky(
            TripEvent(
                LatLngConvertor.getOrigin(startAddress),
                LatLngConvertor.getOrigin(endAddress),
                ssAddress,eeAddress,dDistance,dDuration
            )
        )

        mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
        mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.cameraPosition!!.zoom - 1))
        //Display Layouts
        chipIgnore.visibility = View.VISIBLE
        chipAccept.visibility = View.VISIBLE
        layoutAccept.visibility = View.VISIBLE

        chipIgnore.setOnClickListener {
            val intent = Intent(requireContext(), DriverHomeActivity::class.java)
            startActivity(intent)
        }

        chipAccept.setOnClickListener {
            val intent = Intent(requireContext(), StartTripActivity::class.java)
            intent.putExtra(START_ADDRESS, startAddress)
            intent.putExtra(END_ADDRESS, endAddress)
            startActivity(intent)
        }

        //countdown
        Observable.interval(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                circularProgressBar.progress += 1f
            }
            .takeUntil { aLong -> aLong == "100".toLong() }
            .doOnComplete {

            }.subscribe()
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
        findRoutes(origin, destination)
    }
}


/**
package com.masai.uber.ui.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.directions.route.*
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.maps.android.ui.IconGenerator
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.masai.uber.R
import com.masai.uber.databinding.FragmentDriverHomeBinding
import com.masai.uber.model.eventbus.DriverRequestReceived
import com.masai.uber.remote.IGoogleApi
import com.masai.uber.remote.NetworkRetrofit
import com.masai.uber.remote.RetrofitClient
import com.masai.uber.utlis.Common
import com.masai.uber.utlis.KEY_DRIVER_LOCATION_REFERENCE
import com.masai.uber_rider.remote.ApiClient
import com.masai.uber_rider.remote.models.LegsItem
import com.masai.uber_rider.remote.models.Result
import com.masai.uber_rider.remote.models.RoutesItem
import com.thecode.aestheticdialogs.AestheticDialog
import com.thecode.aestheticdialogs.DialogStyle
import com.thecode.aestheticdialogs.DialogType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_driver_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class DriverHomeFragment : Fragment(), OnMapReadyCallback,
GoogleApiClient.OnConnectionFailedListener,
RoutingListener {
private var binding: FragmentDriverHomeBinding? = null

//location
private var mMap: GoogleMap? = null
private lateinit var mFusedLocationClient: FusedLocationProviderClient
private lateinit var mLocationCallback: com.google.android.gms.location.LocationCallback
private lateinit var mLocationReq: LocationRequest

//firebase
private lateinit var mAuth: FirebaseAuth
private lateinit var onlineRef: DatabaseReference
private var currentUserRef: DatabaseReference? = null
private lateinit var driverLocationRef: DatabaseReference
private var geoFire: GeoFire? = null

private lateinit var userId: String

//Routes
private var compositeDisposable: CompositeDisposable? = null
private var iGoogleApi: IGoogleApi? = null
private var apiClient: ApiClient? = null
private var blackPolyline: Polyline? = null
private var greyPolyline: Polyline? = null
private var polylineOptions: PolylineOptions? = null
private var blackPolylineOptions: PolylineOptions? = null
private var polylineList: ArrayList<LatLng?>? = null
private var originMarker: Marker? = null
private var destinationMarker: Marker? = null

private var selectPlaceEvent: SelectedPlaceEvent? = null

private lateinit var eAddress: String
private var sAddress = "Hanuman Mandir Mattan Sidh 177001"
private lateinit var eeAddress: String
private lateinit var ssAddress: String
private lateinit var dDistance: String
private lateinit var dDuration: String


private lateinit var mapFragment: SupportMapFragment

private val valueEventListener: ValueEventListener = object : ValueEventListener {
override fun onDataChange(snapshot: DataSnapshot) {
if (snapshot.exists() && currentUserRef != null) {
currentUserRef!!.onDisconnect().removeValue()
}
}

override fun onCancelled(error: DatabaseError) {
AestheticDialog.Builder(
requireActivity(),
DialogStyle.TOASTER,
DialogType.ERROR
)
.setTitle("Error ")
.setMessage(error.message.toString())
.show()
}

}


override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
}

override fun onCreateView(
inflater: LayoutInflater, container: ViewGroup?,
savedInstanceState: Bundle?
): View? {
binding = FragmentDriverHomeBinding.inflate(layoutInflater, container, false)

mapFragment = childFragmentManager
.findFragmentById(R.id.map) as SupportMapFragment
mapFragment.getMapAsync(this)
return binding!!.root
}

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
super.onViewCreated(view, savedInstanceState)
initView()
}

private fun initView() {

iGoogleApi = RetrofitClient.getInstance()!!.create(IGoogleApi::class.java)
apiClient = NetworkRetrofit.getInstanceRe()!!.create(ApiClient::class.java)
/** Firebase and GeoFire **/
mAuth = FirebaseAuth.getInstance()
userId = mAuth.currentUser?.uid.toString()
onlineRef = FirebaseDatabase.getInstance().reference
.child(".info/connected")
//        driverLocationRef =
//            FirebaseDatabase.getInstance().getReference(KEY_DRIVER_LOCATION_REFERENCE)
//
//                    currentUserRef = FirebaseDatabase.getInstance().getReference(KEY_DRIVER_LOCATION_REFERENCE)
//                        .child(userId)
//        geoFire = GeoFire(driverLocationRef)

//        registerOnlineSystem()

/** Location **/
mLocationReq = LocationRequest()
mLocationReq.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
mLocationReq.smallestDisplacement = 50f //50m
mLocationReq.interval = 15000 //15 sec
mLocationReq.fastestInterval = 10000 //10 sec

mLocationCallback = object : com.google.android.gms.location.LocationCallback() {
override fun onLocationResult(locationResult: LocationResult) {
super.onLocationResult(locationResult)
val newPosition = LatLng(
locationResult.lastLocation.latitude,
locationResult.lastLocation.longitude
)
mMap?.moveCamera(
CameraUpdateFactory
.newLatLngZoom(newPosition, 18f)
)
val geoCoder = Geocoder(requireContext(), Locale.getDefault())
val addressList: List<Address>?

try {
addressList = geoCoder.getFromLocation(
locationResult.lastLocation.latitude,
locationResult.lastLocation.longitude, 1
)
val cityName = addressList[0].locality
if (cityName != null) {
driverLocationRef =
FirebaseDatabase.getInstance()
.getReference(KEY_DRIVER_LOCATION_REFERENCE)
.child(cityName)
currentUserRef = driverLocationRef.child(
FirebaseAuth.getInstance()
.currentUser!!.uid
)
} else {
driverLocationRef =
FirebaseDatabase.getInstance()
.getReference(KEY_DRIVER_LOCATION_REFERENCE)
.child("Hamirpur")
currentUserRef = driverLocationRef.child(
FirebaseAuth.getInstance()
.currentUser!!.uid
)
}
geoFire = GeoFire(driverLocationRef)
//update location
geoFire?.setLocation(
userId, GeoLocation(
locationResult.lastLocation.latitude,
locationResult.lastLocation.longitude
)
) { key, error ->
if (error != null) {
AestheticDialog.Builder(
requireActivity(),
DialogStyle.TOASTER,
DialogType.ERROR
)
.setTitle("Error ")
.setMessage(error.message.toString())
.show()
} else {
//                        AestheticDialog.Builder(
//                            requireActivity(),
//                            DialogStyle.TOASTER,
//                            DialogType.SUCCESS
//                        )
//                            .setTitle("Success ")
//                            .setMessage("You are online")
//                            .show()
}
}
registerOnlineSystem()
} catch (e: IOException) {
Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()

}

//                //update location
//                geoFire?.setLocation(
//                    userId, GeoLocation(
//                        locationResult.lastLocation.latitude,
//                        locationResult.lastLocation.longitude
//                    )
//                ) { key, error ->
//                    if (error != null) {
//                        AestheticDialog.Builder(
//                            requireActivity(),
//                            DialogStyle.TOASTER,
//                            DialogType.ERROR
//                        )
//                            .setTitle("Error ")
//                            .setMessage(error.message.toString())
//                            .show()
//                    } else {
////                        AestheticDialog.Builder(
////                            requireActivity(),
////                            DialogStyle.TOASTER,
////                            DialogType.SUCCESS
////                        )
////                            .setTitle("Success ")
////                            .setMessage("You are online")
////                            .show()
//                    }
//                }
}

}
mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
if (ActivityCompat.checkSelfPermission(
requireContext(),
android.Manifest.permission.ACCESS_FINE_LOCATION
) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
requireContext(),
android.Manifest.permission.ACCESS_COARSE_LOCATION
) != PackageManager.PERMISSION_GRANTED
) {

return
}

mFusedLocationClient.requestLocationUpdates(
mLocationReq,
mLocationCallback,
Looper.myLooper()!!
)
}

override fun onMapReady(googleMap: GoogleMap) {
this.mMap = googleMap
Dexter.withContext(context)
.withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
.withListener(object : PermissionListener {
override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
if (ActivityCompat.checkSelfPermission(
requireContext(),
android.Manifest.permission.ACCESS_FINE_LOCATION
) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
requireContext(),
android.Manifest.permission.ACCESS_COARSE_LOCATION
) != PackageManager.PERMISSION_GRANTED
) {
return
}


mMap!!.isMyLocationEnabled = true
mMap!!.uiSettings.isMyLocationButtonEnabled = true
mMap!!.setOnMyLocationButtonClickListener {
mFusedLocationClient.lastLocation
.addOnFailureListener { e ->
Snackbar.make(
requireView(), e.message!!,
Snackbar.LENGTH_LONG
).show()
}
.addOnSuccessListener { location ->
val userLating = LatLng(location.latitude, location.longitude)
mMap!!.animateCamera(
CameraUpdateFactory.newLatLngZoom(
userLating,
10f
)
)

}

true
}

/**mMap!!.setOnMyLocationButtonClickListener(
object : GoogleMap.OnMyLocationButtonClickListener {
override fun onMyLocationButtonClick(): Boolean {
if (ActivityCompat.checkSelfPermission(
requireContext(),
android.Manifest.permission.ACCESS_FINE_LOCATION
) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
requireContext(),
android.Manifest.permission.ACCESS_COARSE_LOCATION
) != PackageManager.PERMISSION_GRANTED
) {
return false
}
mFusedLocationClient.lastLocation
.addOnFailureListener {
//                                        AestheticDialog.Builder(
//                                            requireActivity(),
//                                            DialogStyle.TOASTER,
//                                            DialogType.SUCCESS
//                                        )
//                                            .setTitle("${it.message}")
//                                            .show()
}
.addOnSuccessListener {
val userLatLng = LatLng(
it.latitude,
it.longitude
)

mCurrentLocation = it
googleMap.animateCamera(
CameraUpdateFactory
.newLatLngZoom(userLatLng, 18f)
)
}

return true
}
})**/

//set location button
/** val locationButton =
(mapFragment.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(
Integer.parseInt("2")
)**/

val locationButton = (mapFragment.requireView()!!
.findViewById<View>("1".toInt())!!.parent!! as View)
.findViewById<View>("2".toInt())

val params = locationButton.layoutParams as RelativeLayout.LayoutParams
params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
params.bottomMargin = 50

/** val rlp = locationButton.layoutParams as RelativeLayout.LayoutParams
rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
rlp.setMargins(0, 0, 0, 50)**/
}

override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
AestheticDialog.Builder(
requireActivity(),
DialogStyle.TOASTER,
DialogType.WARNING
)
.setTitle("Warning !")
.setMessage("You have denied the location permission")
.show()
}

override fun onPermissionRationaleShouldBeShown(
p0: PermissionRequest?,
p1: PermissionToken?
) {
showRationalDialogForPermissions()
}

}).onSameThread()
.check()

try {
val success = mMap!!.setMapStyle(
MapStyleOptions.loadRawResourceStyle(
context,
R.raw.uber_maps_style
)
)
if (!success) {
Log.d("tag", "error")
}
} catch (e: Exception) {
e.printStackTrace()
Log.d("tag", "error")
}

}

private fun showRationalDialogForPermissions() {
AlertDialog.Builder(requireContext())
.setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
.setPositiveButton(
"GO TO SETTINGS"
) { _, _ ->
try {
Toast.makeText(
requireContext(), "Go to Settings",
Toast.LENGTH_SHORT
).show()
} catch (e: ActivityNotFoundException) {
e.printStackTrace()
}
}
.setNegativeButton("Cancel") { dialog, _ ->
dialog.dismiss()
}.show()
}

private fun registerOnlineSystem() {
onlineRef.addValueEventListener(valueEventListener)
}

override fun onStart() {
EventBus.getDefault().register(this)
super.onStart()
}

override fun onResume() {
super.onResume()
registerOnlineSystem()
}

override fun onPause() {
EventBus.getDefault().unregister(this)
super.onPause()
}

override fun onStop() {
super.onStop()
}

override fun onDestroy() {
mFusedLocationClient.removeLocationUpdates(mLocationCallback)
geoFire?.removeLocation(FirebaseAuth.getInstance().currentUser?.uid)
onlineRef.removeEventListener(valueEventListener)
compositeDisposable?.clear()

if (EventBus.getDefault().hasSubscriberForEvent(DriverRequestReceived::class.java))
EventBus.getDefault().removeStickyEvent(DriverRequestReceived::class.java)
EventBus.getDefault().unregister(this)

super.onDestroy()
binding = null
}

@Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
public fun onDriverRequestReceive(event: DriverRequestReceived) {
selectPlaceEvent = event
if (ActivityCompat.checkSelfPermission(
requireContext(),
Manifest.permission.ACCESS_FINE_LOCATION
) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
requireContext(),
Manifest.permission.ACCESS_COARSE_LOCATION
) != PackageManager.PERMISSION_GRANTED
) {

return
}
mFusedLocationClient.lastLocation
.addOnFailureListener {
it.message?.let { it1 ->
Snackbar.make(
requireView(),
it1, Snackbar.LENGTH_SHORT
).show()
}
}
.addOnSuccessListener {

apiClient?.getDirections(
StringBuilder()
.append(it.latitude)
.append(",")
.append(it.longitude)
.toString(),
event.pickUpLocation, getString(R.string.google_maps_key)
)?.enqueue(object : retrofit2.Callback<Result> {

override fun onResponse(call: Call<Result?>, response: Response<Result?>) {
if (response.isSuccessful) {
val result: Result? = response.body()
val routes: List<RoutesItem?>? = result?.routes
val legs: List<LegsItem?>? = routes?.get(0)?.legs
ssAddress = legs?.get(0)?.startAddress.toString()
eeAddress = legs?.get(0)?.endAddress.toString()
dDistance = legs?.get(0)?.distance?.text.toString()
dDuration = legs?.get(0)?.duration?.text.toString()
val polyline = legs?.get(0)?.steps?.get(0)?.polyline.toString()
polylineList = Common.decodePoly(polyline)
Log.d("tag", ssAddress)
Log.d("tag", eeAddress)
Log.d("tag", dDistance)
Log.d("tag", dDuration)

//                    findRoutes(selectPlaceEvent!!, ssAddress, eeAddress, dDistance, dDuration)

findRoutes(selectPlaceEvent!!)

/** polylineOptions = PolylineOptions()
polylineOptions!!.color(Color.GRAY)
polylineOptions!!.width(12f)
polylineOptions!!.jointType(JointType.ROUND)
polylineOptions!!.addAll(polylineList!!)
greyPolyline = mMap.addPolyline(polylineOptions!!)

blackPolylineOptions = PolylineOptions()
blackPolylineOptions!!.color(Color.BLACK)
blackPolylineOptions!!.width(12f)
blackPolylineOptions!!.jointType(JointType.ROUND)
blackPolylineOptions!!.addAll(polylineList!!)
blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

//Animator
val valueAnimator = ValueAnimator.ofInt(0, 100)
valueAnimator.duration = 1100
valueAnimator.repeatCount = ValueAnimator.INFINITE
valueAnimator.interpolator = LinearInterpolator()
valueAnimator.addUpdateListener { value ->
val points = greyPolyline!!.points
val percentageValue = value.animatedValue.toString().toInt()
val size = points.size
val newPoints = (size * (percentageValue / 100.0f)).toInt()
val p = points.subList(0, newPoints)
blackPolyline!!.points = p
}
valueAnimator.start()

val latLngBound = LatLngBounds.Builder().include(selectPlaceEvent.origin)
.include(selectPlaceEvent.destination).build()

addOriginMarker(dDuration, ssAddress)
addDestinationMarker(dDuration, eeAddress)

mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom - 1)) **/
}

}

override fun onFailure(call: Call<Result?>, t: Throwable) {
Log.d("tag", "Exception")
}

})


/** iGoogleApi?.getDirections(
"driving",
"less_driving",
StringBuilder()
.append(it.latitude)
.append(",")
.append(it.longitude)
.toString(),
event.pickUpLocation, getString(R.string.google_maps_key)
)
?.subscribeOn(io.reactivex.schedulers.Schedulers.io())
?.observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
?.subscribe { result ->

try {
val jsonObj = JSONObject(result!!)
val jsonArray = jsonObj.getJSONArray("routes")

for (i in 0 until jsonArray.length()) {
val route = jsonArray.getJSONObject(i)
val poly = route.getJSONObject("overview_polyline")
val polyline = poly.getString("points")
polylineList = Common.decodePoly(polyline)
}
polylineOptions = PolylineOptions()
polylineOptions!!.color(Color.GRAY)
polylineOptions!!.width(12f)
polylineOptions!!.jointType(JointType.ROUND)
polylineOptions!!.startCap(SquareCap())
polylineOptions!!.addAll(polylineList!!)
greyPolyline = mMap?.addPolyline(polylineOptions!!)

blackPolylineOptions = PolylineOptions()
blackPolylineOptions!!.color(Color.BLACK)
blackPolylineOptions!!.width(12f)
blackPolylineOptions!!.jointType(JointType.ROUND)
blackPolylineOptions!!.startCap(SquareCap())
blackPolylineOptions!!.addAll(polylineList!!)
blackPolyline = mMap?.addPolyline(blackPolylineOptions!!)

//Animator
val valueAnimator = ValueAnimator.ofInt(0, 100)
valueAnimator.duration = 1100
valueAnimator.repeatCount = ValueAnimator.INFINITE
valueAnimator.interpolator = LinearInterpolator()
valueAnimator.addUpdateListener { value ->
val points = greyPolyline!!.points
val percentageValue = value.animatedValue.toString().toInt()
val size = points.size
val newPoints = (size * (percentageValue / 100.0f)).toInt()
val p = points.subList(0, newPoints)
blackPolyline!!.points = p
}
valueAnimator.start()

var origin =
LatLng(it.latitude, it.longitude)

var destination =
LatLng(
event?.pickUpLocation!!.split(",")[0].toDouble(),
event?.pickUpLocation!!.split(",")[1].toDouble()
)

val latLngBound =
LatLngBounds.Builder().include(origin)
.include(destination).build()

//add car icon for origin

val objects = jsonArray.getJSONObject(0)
val legs = objects.getJSONArray("legs")
val legsObject = legs.getJSONObject(0)

val time = legsObject.getJSONObject("duration")
val duration = time.getString("text")

val distanceEstimator = legsObject.getJSONObject("distance")
val distance = distanceEstimator.getString("text")

tvEstimateTime.text = duration.toString()
tvEstimatedDistance.text = distance.toString()

mMap?.addMarker(
MarkerOptions().position(destination)
.icon(BitmapDescriptorFactory.defaultMarker())
.title("Pickup Location")
)

mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.cameraPosition!!.zoom - 1))


//Display Layouts
chipIgnore.visibility = View.VISIBLE
layoutAccept.visibility = View.VISIBLE

//countdown
Observable.interval(100, TimeUnit.MILLISECONDS)
.observeOn(AndroidSchedulers.mainThread())
.doOnNext {
circularProgressBar.progress += 1f
}
.takeUntil { aLong -> aLong == "100".toLong() }
.doOnComplete {
Toast.makeText(
context,
"Fake accept action ",
Toast.LENGTH_SHORT
).show()
}.subscribe()


} catch (e: Exception) {
e.printStackTrace()
}
}?.let {
compositeDisposable?.add(
it
)
} **/

}
/**    .addOnSuccessListener {
val locationStr= StringBuilder()
.append(it.latitude)
.append(",")
.append(it.longitude)

compositeDisposable.add(iGoogleApi.getDirections("driving",
"less_driving",
locationStr.toString(),
event.pickUpLocation,

))
} **/
}

private fun findRoutes(
) {

val routing = Routing.Builder()
.travelMode(AbstractRouting.TravelMode.DRIVING)
.withListener(this)
.alternativeRoutes(true)
.waypoints(selectPlaceEvent.origin, selectPlaceEvent.destination)
.key("AIzaSyBrgPQhpR6cePo-zHYSxNfEwQY6MqNI74w") //also define your api key here.
.build()
routing.execute()
}

override fun onConnectionFailed(p0: ConnectionResult) {
findRoutes(selectPlaceEvent!!)
}

override fun onRoutingFailure(p0: RouteException?) {
findRoutes(selectPlaceEvent!!)
}

override fun onRoutingStart() {

}

override fun onRoutingCancelled() {
findRoutes(selectPlaceEvent!!)
}

override fun onRoutingSuccess(p0: java.util.ArrayList<Route>?, p1: Int) {
val center = CameraUpdateFactory.newLatLng(selectPlaceEvent!!.origin)
val zoom = CameraUpdateFactory.zoomTo(16f)
polylines?.clear()
val polyOptions = PolylineOptions()
var polylineStartLatLng: LatLng? = null
var polylineEndLatLng: LatLng? = null
polylines = ArrayList()
//add route(s) to the map using polyline
for (i in 0 until p0!!.size) {
if (i == p1) {
polyOptions.color(Color.BLACK)
polyOptions.width(12f)
polyOptions.addAll(p0.get(p1).getPoints())
val polyline = mMap.addPolyline(polyOptions)
polylineStartLatLng = polyline.points[0]
val k = polyline.points.size
polylineEndLatLng = polyline.points[k - 1]
(polylines as ArrayList<Polyline>).add(polyline)
} else {
}
}
//Add Marker on route starting position
//Add Marker on route starting position
val view = layoutInflater.inflate(com.directions.route.R.layout.origin_infor_window, null)
val tvTime = view.findViewById<View>(com.directions.route.R.id.tvTime) as TextView
tvOrigin = view.findViewById<View>(com.directions.route.R.id.tvOrigin) as TextView
tvTime.text = Common.formatDuration(dDuration.toString()).toString()
tvOrigin!!.text = ssAddress

val generator = IconGenerator(this)
generator.setContentView(view)
generator.setBackground(ColorDrawable(Color.TRANSPARENT))
val icon = generator.makeIcon()

originMarker = mMap.addMarker(
MarkerOptions()
.icon(BitmapDescriptorFactory.fromBitmap(icon))
.position(selectPlaceEvent!!.origin)
)

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

val endMarker = MarkerOptions()
endMarker.position(polylineEndLatLng!!)
endMarker.title("Destination")
mMap.addMarker(endMarker)
}

private fun drawPath2(selectPlaceEvent: SelectedPlaceEvent) {

val apiClient = NetworkRetrofit.getInstanceRe()?.create(ApiClient::class.java)


apiClient?.getDirections(
selectPlaceEvent.originString,
selectPlaceEvent.destinationString,
getString(com.directions.route.R.string.google_maps_key)
)?.enqueue(object : retrofit2.Callback<Result> {

override fun onResponse(call: Call<Result?>, response: Response<Result?>) {
if (response.isSuccessful) {
val result: Result? = response.body()
val routes: List<RoutesItem?>? = result?.routes
val legs: List<LegsItem?>? = routes?.get(0)?.legs
ssAddress = legs?.get(0)?.startAddress.toString()
eeAddress = legs?.get(0)?.endAddress.toString()
dDistance = legs?.get(0)?.distance?.text.toString()
dDuration = legs?.get(0)?.duration?.text.toString()
val polyline = legs?.get(0)?.steps?.get(0)?.polyline.toString()
polylineList = Common.decodePoly(polyline)
Log.d("tag", ssAddress)
Log.d("tag", eeAddress)
Log.d("tag", dDistance)
Log.d("tag", dDuration)

//                    findRoutes(selectPlaceEvent!!, ssAddress, eeAddress, dDistance, dDuration)

findRoutes(selectPlaceEvent!!)

/** polylineOptions = PolylineOptions()
polylineOptions!!.color(Color.GRAY)
polylineOptions!!.width(12f)
polylineOptions!!.jointType(JointType.ROUND)
polylineOptions!!.addAll(polylineList!!)
greyPolyline = mMap.addPolyline(polylineOptions!!)

blackPolylineOptions = PolylineOptions()
blackPolylineOptions!!.color(Color.BLACK)
blackPolylineOptions!!.width(12f)
blackPolylineOptions!!.jointType(JointType.ROUND)
blackPolylineOptions!!.addAll(polylineList!!)
blackPolyline = mMap.addPolyline(blackPolylineOptions!!)

//Animator
val valueAnimator = ValueAnimator.ofInt(0, 100)
valueAnimator.duration = 1100
valueAnimator.repeatCount = ValueAnimator.INFINITE
valueAnimator.interpolator = LinearInterpolator()
valueAnimator.addUpdateListener { value ->
val points = greyPolyline!!.points
val percentageValue = value.animatedValue.toString().toInt()
val size = points.size
val newPoints = (size * (percentageValue / 100.0f)).toInt()
val p = points.subList(0, newPoints)
blackPolyline!!.points = p
}
valueAnimator.start()

val latLngBound = LatLngBounds.Builder().include(selectPlaceEvent.origin)
.include(selectPlaceEvent.destination).build()

addOriginMarker(dDuration, ssAddress)
addDestinationMarker(dDuration, eeAddress)

mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom - 1)) **/
}

}

override fun onFailure(call: Call<Result?>, t: Throwable) {
Log.d("tag", "Exception")
}

})


}
}

 **/