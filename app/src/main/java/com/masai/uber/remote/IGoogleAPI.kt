package com.masai.uber.remote

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query


public interface IGoogleApi {
    @GET("maps/api/direction/json")
    fun getDirections(
        @Query("mode") mode: String?,
        @Query("transit_routing_preference") transit_routing: String?,
        @Query("origin") from: String?,
        @Query("destination") to: String?,
        @Query("key") key: String?
    ): Observable<String?>?
}