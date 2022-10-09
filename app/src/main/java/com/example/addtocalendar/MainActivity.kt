package com.example.addtocalendar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.fb
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var dateParsed: Date = Date()
    private var imgPhoto: ImageView? = null
    private var retrofit: Retrofit? = null
    private var imgLink: String = "null"
    private var bitmap: Bitmap? = null

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) {

        val stream: InputStream? = this.contentResolver.openInputStream(Uri.parse(it.toString()))
        val bufferStream = BufferedInputStream(stream)
        bitmap = BitmapFactory.decodeStream(bufferStream)

        imgPhoto!!.setImageBitmap(bitmap)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val database = FirebaseDatabase.getInstance().getReference("date")
        val btnPickDate = findViewById<Button>(R.id.btn_pick_date)
        val tvDate = findViewById<TextView>(R.id.tv_date)
        imgPhoto = findViewById(R.id.img_photo)
        imgPhoto!!.clipToOutline = true

        btnPickDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .build()
            datePicker.addOnPositiveButtonClickListener {
                dateParsed = Date(datePicker.selection!!.toLong())
                tvDate.text = SimpleDateFormat("dd MMMM yyyy").format(dateParsed).toString()
            }
            datePicker.show(supportFragmentManager, "date")
        }

        val edTitle: EditText = findViewById(R.id.ed_title)
        val edDescription: EditText = findViewById(R.id.ed_desription)
        val btnAddDate: Button = findViewById(R.id.btn_add)
        val btnLoadImg: Button = findViewById(R.id.btn_load_img)

        btnLoadImg.setOnClickListener {
            getImage.launch("image/*")
        }

        btnAddDate.setOnClickListener()
        {
            if (tvDate.text.toString() == "" || edDescription.text.toString() == "" || edTitle.text.toString() == "") {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val titleFromField: String = edTitle.text.toString()
            val descriptionFromField: String = edDescription.text.toString()
            val date = SimpleDateFormat("dd.MM.yyyy").format(dateParsed).toString()
            val newDate = fb()

            if (bitmap != null) {
                val filePath = File.createTempFile("img", ".jpg").path
                val file = File(filePath)
                val bos = ByteArrayOutputStream()
                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                val bitMapData = bos.toByteArray()
                val fos = FileOutputStream(file)
                fos.write(bitMapData)
                fos.flush()
                fos.close()

                val api = "00001ba9a6d104f9fc28483af24f5c16"
                val baseUrl = "https://thumbsnap.com/api/"

                val request = kotlinx.coroutines.MainScope()
                request.launch {
                    withContext(IO) {uploadImage(baseUrl, Uri.parse(filePath), api, this@MainActivity)}
                    Log.i("aboba", imgLink)
                    newDate.fb(
                        date = date,
                        title = titleFromField,
                        desc = descriptionFromField,
                        img = imgLink
                    )

                    database.push().setValue(newDate).addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "Дата сохранена", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                    }
                }

            } else {
                newDate.fb(
                    date = date,
                    title = titleFromField,
                    desc = descriptionFromField,
                    img = imgLink
                )

                database.push().setValue(newDate).addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Дата сохранена", Toast.LENGTH_LONG).show()
                }.addOnFailureListener {
                    Toast.makeText(this@MainActivity, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    interface UploadAPI {
        @Multipart
        @POST("upload")
        fun uploadImage(
            @Part file: MultipartBody.Part?,
            @Part("key") requestBody: RequestBody?
        ): Call<ResponseBody?>?
    }

    fun uploadImage(baseUrl: String, fileUri: Uri, api: String, context: Context) {
        if (retrofit == null) {
            val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        val file = File(fileUri.toString())

        val client: UploadAPI = retrofit!!.create(UploadAPI::class.java)

        val requestFile: RequestBody =
            RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val multipartBody: MultipartBody.Part =
            MultipartBody.Part.createFormData("media", file.name, requestFile)
        val key: RequestBody = RequestBody.create(MultipartBody.FORM, api)
        val call: Call<ResponseBody?>? = client.uploadImage(multipartBody, key)
        val response: Response<ResponseBody?> = call!!.execute()
        val json = JSONObject(response.body()!!.string())
        val jsonData = json.getJSONObject("data")
        imgLink = jsonData.getString("media")
    }
}


