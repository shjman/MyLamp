package com.yahorshymanchyk.mylamp.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.yahorshymanchyk.mylamp.secrets.Secrets
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object HomeAssistantClient {
    val api: HomeAssistantApi by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val okHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    val authorizedRequest =
                        chain
                            .request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer ${Secrets.HA_LONG_LIVED_TOKEN}")
                            .build()
                    chain.proceed(authorizedRequest)
                }.build()

        val retrofit =
            Retrofit
                .Builder()
                .baseUrl("${Secrets.HA_BASE_URL}/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

        retrofit.create(HomeAssistantApi::class.java)
    }
}
