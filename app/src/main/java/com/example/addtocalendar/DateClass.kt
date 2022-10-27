package com.example.addtocalendar

import com.google.gson.annotations.SerializedName

data class DateClass(
    @SerializedName("id")
    val id: Int,

    @SerializedName("day")
    val day: Int,

    @SerializedName("month")
    val month: Int,

    @SerializedName("year")
    val year: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("image")
    val photo: String
    )