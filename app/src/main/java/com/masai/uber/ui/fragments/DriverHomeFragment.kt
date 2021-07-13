package com.masai.uber.ui.fragments

import android.Manifest
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
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
import com.masai.uber.remote.RetrofitClient
import com.masai.uber.utlis.Common
import com.masai.uber.utlis.KEY_DRIVER_LOCATION_REFERENCE
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
import java.io.IOException
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class DriverHomeFragment : Fragment(), OnMapReadyCallback {
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
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineList: ArrayList<LatLng?>? = null


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

                iGoogleApi?.getDirections(
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
                    }

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
}