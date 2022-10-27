package com.example.addtocalendar

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitServices {
    @GET("get_dates")
    fun getDatesList(): Call<ArrayList<DateClass>>

    @Multipart
    @POST("load_image")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part id: MultipartBody.Part
    ): Call<ResponseBody?>?

    @POST("add_date")
    fun addingDate(
        @Body date: DateClass
    ): Call<DateClass>
}