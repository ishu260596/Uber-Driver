package com.masai.uber_rider.remote

import com.masai.uber_rider.remote.models.Result
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiClient {
    @GET("/maps/api/directions/json")
    fun getDirections(
        @Query("origin") from: String?,
        @Query("destination") to: String?,
        @Query("key") key: String?
    ): Call<Result>
}