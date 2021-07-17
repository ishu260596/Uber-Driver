package com.masai.uber.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkRetrofit {
    val instance: Retrofit? = null
    @JvmStatic
    fun getInstanceRe(): Retrofit? {
        return NetworkRetrofit.instance
            ?: Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}