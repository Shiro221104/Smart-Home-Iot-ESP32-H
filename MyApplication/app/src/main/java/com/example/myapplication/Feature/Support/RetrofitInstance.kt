package com.example.myapplication.Feature.Support

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val api: DialogflowApi by lazy {

        Retrofit.Builder()
            .baseUrl("https://dialogflow.googleapis.com/")
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(DialogflowApi::class.java)
    }
}