package com.example.addtocalendar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Response

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
            var mode = 0
            var year = 0
            var month = 0
            var day = 0
            if (edYear.text.isNotEmpty()) {
                mode = 1
                year = edYear.text.toString().toInt()
            }
            if (edYear.text.isNotEmpty() && spMonth.selectedItemPosition > 0) {
                mode = 2
                year = edYear.text.toString().toInt()
                month = spMonth.selectedItemPosition
            }
            if (edYear.text.isNotEmpty() && spMonth.selectedItemPosition > 0 && edDay.text.isNotEmpty()) {
                mode = 3
                year = edYear.text.toString().toInt()
                month = spMonth.selectedItemPosition
                day = edDay.text.toString().toInt()
            }
            if (mode > 0) {
                for (i in dates) {
                    when (mode) {
                        1 ->
                            if (year == i.year) {
                                searchData.add(i)
                            }
                        2 ->
                            if (year == i.year && month == i.month) {
                                searchData.add(i)
                            }
                        3 ->
                            if (year == i.year && month == i.month && day == i.day) {
                                searchData.add(i)
                            }
                    }

                }
                if (searchData.isEmpty()) {
                    var firstDate = ""
                    var secondDate = ""
                    for (i in dates) {
                        if (year > i.year) {
                            if (i.month > 0) {
                                firstDate =
                                    "${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                            if (i.month > 0 && i.day > 0) {
                                firstDate =
                                    "${i.day} ${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                            break
                        }
                        if (year < i.year) {
                            if (i.month > 0) {
                                secondDate =
                                    "${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                            if (i.month > 0 && i.day > 0) {
                                secondDate =
                                    "${i.day} ${MonthAdapter.getMonthByInt(i.month)} ${i.year}"
                            }
                        }
                    }
                    if (secondDate.isNotEmpty() and firstDate.isNotEmpty()) {
                        Toast.makeText(
                            this,
                            "Ближайшие даты $firstDate и $secondDate",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else if (secondDate.isNotEmpty()) {
                        Toast.makeText(
                            this,
                            "Ближайшая дата $secondDate",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else if (firstDate.isNotEmpty()) {
                        Toast.makeText(
                            this,
                            "Ближайшая дата $firstDate",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    updateRecyclerView(searchData)
                }
            } else {
                updateRecyclerView(dates)
            }
        }
    }

    fun updateRecyclerView(dates: ArrayList<DateClass>) {
        rvDates?.layoutManager = LinearLayoutManager(this)
        val recyclerAdapter = RecyclerAdapter(dates)
        rvDates?.adapter = recyclerAdapter
    }

    fun loadData(prefs: SharedPreferences, context: Context) {
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
                    Toast.makeText(context, "Не удалось подключиться к серверу", Toast.LENGTH_LONG).show()
                }
            })
    }
}