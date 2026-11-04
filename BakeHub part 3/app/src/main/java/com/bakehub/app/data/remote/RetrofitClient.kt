package com.bakehub.app.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://www.themealdb.com/api/json/v1/1/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val recipeApi: RecipeApi by lazy {
        retrofit.create(RecipeApi::class.java)
    }
}