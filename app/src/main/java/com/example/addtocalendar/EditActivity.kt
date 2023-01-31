package com.example.addtocalendar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.os.IResultReceiver._Parcel
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Response
import java.util.Date

class EditActivity : AppCompatActivity() {

    private var dates: ArrayList<DateClass> = ArrayList()
    private var searchData: ArrayList<DateClass> = ArrayList()

    private var url: String = ""
    private var secretKey: String? = null

    private var rvDates: RecyclerView? = null
    private var pbLoading: ProgressBar? = null

    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        rvDates = findViewById(R.id.rvDates)

        val spMonth: Spinner = findViewById(R.id.spMonth)
        val edDay: EditText = findViewById(R.id.edDay)
        val edYear: EditText = findViewById(R.id.edYear)
        val btnSearch: ImageButton = findViewById(R.id.btnSearch)
        pbLoading = findViewById(R.id.pbLoading)

        val btnBack: ImageButton = findViewById(R.id.btnReturnBack)
        btnBack.setOnClickListener { finish() }

        val btnUpdate: TextView = findViewById(R.id.btnUpdate)
        btnUpdate.setOnClickListener {
            dates.clear()
            loadData(prefs, this)
        }

        val months = MonthAdapter.getMonthArray()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, months)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spMonth.adapter = adapter

        prefs = getSharedPreferences("url", Context.MODE_PRIVATE)
        secretKey = prefs.getString("secret_key", "museum_1901")
        if (prefs.contains("server_url")) {
            loadData(prefs, this)
        } else {
            Toast.makeText(this, "Нет адреса для подключения к серверу", Toast.LENGTH_LONG).show()
        }

        btnSearch.setOnClickListener {
            searchData.clear()
            val year = if (edYear.text.isNotEmpty()) { edYear.text.toString().toInt() } else { 0 }
            val month = spMonth.selectedItemPosition
            val day = if (edDay.text.isNotEmpty()) { edDay.text.toString().toInt() } else { 0 }
            if (year == 0 && day == 0 && month == 0){
                updateRecyclerView(dates)
                return@setOnClickListener
            } else {
                for (i in dates) {
                    if (((i.year == year) && (i.year != 0)) ||
                        ((i.day == day) && (i.day != 0)) ||
                        ((i.month == month) && (i.month != 0))) {
                        searchData.add(i)
                    }
                }
            }
            if (searchData.isEmpty()) {
                Toast.makeText(this@EditActivity, "Не нашлось событий с такой датой", Toast.LENGTH_LONG).show()
            } else {
                updateRecyclerView(searchData)
            }
        }
    }

    fun updateRecyclerView(dates: ArrayList<DateClass>) {
        rvDates?.layoutManager = LinearLayoutManager(this)
        val recyclerAdapter = RecyclerAdapter(dates)
        rvDates?.adapter = recyclerAdapter
    }

    private fun loadData(prefs: SharedPreferences, context: Context) {
        url = prefs.getString("server_url", "None").toString()
        val baseUrl = "$url/"
        val retrofitServices: RetrofitServices =
            RetrofitClient.getClient(baseUrl).create(RetrofitServices::class.java)

        pbLoading?.visibility = View.VISIBLE

        retrofitServices.getDatesList()
            .enqueue(object : retrofit2.Callback<ArrayList<DateClass>> {
                override fun onResponse(
                    call: Call<ArrayList<DateClass>>,
                    response: Response<ArrayList<DateClass>>
                ) {
                    val items = response.body()
                    if (items != null) {
                        dates = items
                        updateRecyclerView(dates)
                        pbLoading?.visibility = View.GONE
                    }
                }

                override fun onFailure(call: Call<ArrayList<DateClass>>, t: Throwable) {
                    Toast.makeText(context, "Не удалось подключиться к серверу", Toast.LENGTH_LONG)
                        .show()
                }
            })
    }
}