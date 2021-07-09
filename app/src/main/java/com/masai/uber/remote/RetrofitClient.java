package com.masai.uber.remote;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

class RetrofitClient {
    private static Retrofit instance;

    private static Retrofit getInstance() {
        return instance == null ? new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build() : instance;
    }
}
