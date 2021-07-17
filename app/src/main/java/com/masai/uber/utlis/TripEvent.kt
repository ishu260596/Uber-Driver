package com.masai.uber.utlis

import com.google.android.gms.maps.model.LatLng
import java.lang.StringBuilder

data class TripEvent(
    var origin: LatLng,
    var destination: LatLng,
    val ssAddress: String,
   val eeAddress: String,
    val dDistance: String,
    val dDuration: String
) {
    val originString: String
        get() = StringBuilder()
            .append(origin.latitude)
            .append(",")
            .append(origin.longitude)
            .toString()

    val destinationString: String
        get() = StringBuilder()
            .append(destination.latitude)
            .append(",")
            .append(destination.longitude)
            .toString()

}
