package com.example.myapplication

class fb {
    var date: String? = null
    var title: String? = null
    var desc: String? = null
    var img: String? = null

    fun fb(
        date: String?,
        title: String?,
        desc: String?,
        img: String?,
    ) {
        this.date = date
        this.title = title
        this.desc = desc
        this.img = img
    }

    fun getDates(): String? {
        return date
    }
    fun getTitles(): String? {
        return title
    }
    fun getDescription(): String? {
        return desc
    }
    fun getImage(): String? {
        return img
    }
}