package com.masai.uber.utlis

import com.google.android.gms.maps.model.LatLng

object LatLngConvertor {

    fun getOrigin(origin: String): LatLng {
        val originLatLng = origin.split(",".toRegex()).toTypedArray()
        val latitudeOrigin = originLatLng[0].toDouble()
        val longitudeOrigin = originLatLng[1].toDouble()
        return LatLng(latitudeOrigin, longitudeOrigin)

    }

    fun getDestination(destination: String): LatLng {
        val destinationLatLng = destination.split(",".toRegex()).toTypedArray()
        val latitudeDestination = destinationLatLng[0].toDouble()
        val longitudeDestination = destinationLatLng[1].toDouble()
        return LatLng(latitudeDestination, longitudeDestination)

    }
}