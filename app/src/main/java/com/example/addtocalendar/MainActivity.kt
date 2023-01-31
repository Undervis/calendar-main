package com.example.addtocalendar

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import retrofit2.Call
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private var imgPhoto: ImageView? = null
    private var bitmap: Bitmap? = null

    private var url: String = ""
    private var secretKey: String? = null

    val NO_URL_DIALOG: Int = 0
    val CONNECTION_ERROR_DIALOG: Int = 1
    val SECRET_KEY_DIALOG: Int = 2
    val SET_SECRET_KEY_DIALOG: Int = 3
    val NEW_URL_DIALOG: Int = 4

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

        window.statusBarColor = resources.getColor(R.color.base_dark)

        imgPhoto = findViewById(R.id.img_photo)
        imgPhoto!!.clipToOutline = true

        val edTitle: EditText = findViewById(R.id.ed_title)
        val edDescription: EditText = findViewById(R.id.ed_desription)
        val btnAddDate: Button = findViewById(R.id.btn_add)
        val btnLoadImg: Button = findViewById(R.id.btn_load_img)

        val btnEdit: Button = findViewById(R.id.btn_edit)
        btnEdit.setOnClickListener {
            val intent = Intent(this, EditActivity::class.java)
            startActivity(intent)
        }

        val spMonth: Spinner = findViewById(R.id.spMonth)
        val edDay: EditText = findViewById(R.id.edDay)
        val edYear: EditText = findViewById(R.id.edYear)

        val months = MonthAdapter.getMonthArray()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, months)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spMonth.adapter = adapter

        val prefs = getSharedPreferences("url", Context.MODE_PRIVATE)
        secretKey = prefs.getString("secret_key", "museum_1901")

        val tvConnectionStatus: TextView = findViewById(R.id.tvConnectionStatus)
        val pbConnectionCheck: ProgressBar = findViewById(R.id.pbConnectionCheck)

        if (prefs.contains("server_url")) {
            url = prefs.getString("server_url", "None").toString()
            val checkConnection = kotlinx.coroutines.MainScope()
            checkConnection.launch {
                val connection: Boolean
                withContext(IO) { connection = testConnection(url) }
                pbConnectionCheck.visibility = View.INVISIBLE
                if (connection) {
                    tvConnectionStatus.visibility = View.INVISIBLE
                } else {
                    tvConnectionStatus.text = "Нет подключения к серверу"
                    callingDialog(this@MainActivity, CONNECTION_ERROR_DIALOG, prefs)
                }
            }
        } else {
            callingDialog(this, NO_URL_DIALOG, prefs)
            pbConnectionCheck.visibility = View.INVISIBLE
            tvConnectionStatus.visibility = View.INVISIBLE
        }

        btnLoadImg.setOnClickListener {
            getImage.launch("image/*")
        }

        btnAddDate.setOnClickListener()
        {
            if ((edYear.text.isEmpty() && edDay.text.isEmpty() && spMonth.selectedItemPosition == 0)
                || edDescription.text.isEmpty() || edTitle.text.isEmpty()
            ) {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (edDay.text.isNotEmpty()) {
                if (edDay.text.toString().toInt() > 31 || edDay.text.toString().toInt() < 1) {
                    Toast.makeText(this, "Недопустимый день месяца", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            val retrofitServices: RetrofitServices =
                RetrofitClient.getClient(url).create(RetrofitServices::class.java)

            val day = if (edDay.text.isNotEmpty()) { edDay.text.toString().toInt() } else { 0 }
            val month = spMonth.selectedItemPosition
            val year = if (edYear.text.isNotEmpty()) { edYear.text.toString().toInt() } else { 0 }
            val title: String = edTitle.text.toString()
            val description: String = edDescription.text.toString()

            val date = DateClass(0, day, month, year, title, description, "null")

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

                val request = kotlinx.coroutines.MainScope()
                request.launch {
                    var id: Int
                    var imgResult: Boolean
                    withContext(IO) {
                        id = addingDate(retrofitServices, date)
                    }
                    if (id == -1) {
                        Toast.makeText(
                            this@MainActivity,
                            "Не удалось сохранить дату",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        withContext(IO) {
                            imgResult = uploadImage(retrofitServices, Uri.parse(filePath), id)
                        }
                        if (imgResult) {
                            edDay.text.clear()
                            edYear.text.clear()
                            edTitle.text.clear()
                            edDescription.text.clear()
                            spMonth.setSelection(0)
                            Glide.with(this@MainActivity).load(R.drawable.no_image_placer).into(imgPhoto!!)
                            Toast.makeText(
                                this@MainActivity,
                                "Дата сохранена",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Дата сохранена, но не удалось загрузить изображение",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } else {
                val request = kotlinx.coroutines.MainScope()
                request.launch {
                    var id: Int
                    withContext(IO) {
                        id = addingDate(retrofitServices, date)
                    }
                    if (id == -1) {
                        Toast.makeText(
                            this@MainActivity,
                            "Не удалось сохранить дату",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        edDay.text.clear()
                        edYear.text.clear()
                        edTitle.text.clear()
                        edDescription.text.clear()
                        spMonth.setSelection(0)
                        Toast.makeText(
                            this@MainActivity,
                            "Дата сохранена",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        }
    }

    fun addingDate(services: RetrofitServices, date: DateClass): Int {
        return try {
            val response = services.addingDate(date).execute()
            response.body()?.id!!
        } catch (E: IOException) {
            -1
        }
    }

    fun uploadImage(services: RetrofitServices, fileUri: Uri, id: Int): Boolean {
        val file = File(fileUri.toString())

        val requestFile: RequestBody =
            RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val filePart: MultipartBody.Part =
            MultipartBody.Part.createFormData("file", file.name, requestFile)
        val idPart: MultipartBody.Part =
            MultipartBody.Part.createFormData("id", id.toString())
        val call: Call<ResponseBody> = services.uploadImage(filePart, idPart)
        return try {
            call.execute()
            true
        } catch (E: IOException) {
            false
        }

    }

    private fun testConnection(url: String): Boolean {
        val baseUrl = "$url/"
        val retrofitServices: RetrofitServices =
            RetrofitClient.getClient(baseUrl).create(RetrofitServices::class.java)

        return try {
            val request = retrofitServices.getDatesList().execute()
            Log.i("retrofit", request.body().toString())
            request.body()?.isNotEmpty() == true
        } catch (E: IOException) {
            Log.e("retrofit", E.message.toString())
            false
        }
    }

    @SuppressLint("SetTextI18n")
    fun callingDialog(context: Context, mode: Int, prefs: SharedPreferences): Boolean {

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_layout)

        var result = true

        val btnAccept: TextView = dialog.findViewById(R.id.btnAccept)
        val btnCancel: TextView = dialog.findViewById(R.id.btnCancel)
        val tvGeneralMessage: TextView = dialog.findViewById(R.id.tvMessageDialog)
        val tvSecondMessage: TextView = dialog.findViewById(R.id.tvMessageScnd)
        val edTextField: EditText = dialog.findViewById(R.id.edTextField)
        val edConfirmSK: EditText = dialog.findViewById(R.id.edConfirmSK)
        val imDialogIcon: ImageView = dialog.findViewById(R.id.imDialogIcon)
        val tvErrorMsg: TextView = dialog.findViewById(R.id.tvErrorMsg)
        val pbConnection: ProgressBar = dialog.findViewById(R.id.pbConnection)

        if (mode == NO_URL_DIALOG || mode == NEW_URL_DIALOG) {
            if (mode == NO_URL_DIALOG) {
                tvGeneralMessage.text = "Не задан URL для подключения к серверу"
                tvSecondMessage.text = "Укажите новый адресс для подключения"
            }
            if (mode == NEW_URL_DIALOG) {
                tvGeneralMessage.text = "Не удалось подключиться к серверу"
                tvSecondMessage.text = "Укажите новый адресс для подключения"
            }

            btnAccept.setOnClickListener {
                if (edTextField.text.isNotEmpty()) {
                    var newUrl = edTextField.text.toString()
                    if (!newUrl.contains("http://") || !newUrl.contains("https://")) {
                        newUrl = "http://$newUrl"
                    }

                    pbConnection.visibility = View.VISIBLE
                    tvErrorMsg.visibility = View.INVISIBLE

                    val test = kotlinx.coroutines.MainScope()
                    test.launch {
                        var response = false
                        withContext(IO) { response = testConnection(newUrl) }
                        if (response) {
                            pbConnection.visibility = View.INVISIBLE
                            result = if (mode == NO_URL_DIALOG) {
                                if (callingDialog(context, SET_SECRET_KEY_DIALOG, prefs)) {
                                    prefs.edit().putString("server_url", newUrl).apply()
                                    true
                                } else {
                                    false
                                }
                            } else {
                                prefs.edit().putString("server_url", newUrl).apply()
                                true
                            }
                            dialog.dismiss()
                        } else {
                            pbConnection.visibility = View.INVISIBLE
                            tvErrorMsg.visibility = View.VISIBLE
                            tvErrorMsg.text = "Не удалось подключиться"
                        }
                    }
                    return@setOnClickListener
                } else {
                    tvErrorMsg.visibility = View.VISIBLE
                    tvErrorMsg.text = "Поле пустое"
                }

            }
        }

        if (mode == CONNECTION_ERROR_DIALOG) {
            tvGeneralMessage.text = "Не удалось подключиться к серверу"
            tvSecondMessage.text = "Хотите изменить адресс подключения?"
            edTextField.visibility = View.GONE

            btnAccept.setOnClickListener {
                result = callingDialog(context, SECRET_KEY_DIALOG, prefs)
                dialog.dismiss()
                return@setOnClickListener
            }
        }

        if (mode == SECRET_KEY_DIALOG) {
            tvGeneralMessage.text = "Введите секретный ключ"
            tvSecondMessage.visibility = View.GONE
            imDialogIcon.setImageDrawable(resources.getDrawable(R.drawable.key_icon))

            edTextField.hint = "Секретный ключ"
            edTextField.transformationMethod = PasswordTransformationMethod.getInstance()

            btnAccept.text = "Подтвердить"
            btnAccept.setOnClickListener {
                if (edTextField.text.isNotEmpty()) {
                    if (edTextField.text.toString() == secretKey) {
                        result = callingDialog(context, NEW_URL_DIALOG, prefs)
                        dialog.dismiss()
                        return@setOnClickListener
                    } else {
                        edTextField.text = edConfirmSK.text
                        tvErrorMsg.visibility = View.VISIBLE
                        tvErrorMsg.text = "Неверный секретный ключ"
                    }
                } else {
                    tvErrorMsg.visibility = View.VISIBLE
                    tvErrorMsg.text = "Поле пустое"
                }
            }
        }

        if (mode == SET_SECRET_KEY_DIALOG) {
            tvGeneralMessage.text =
                "В случае сбоя подключения, понадобится секретный ключ для изменения адреса подключения." +
                        "\nЗапомните его, изменить или восстановить секретный ключ невозможно"
            tvSecondMessage.text = "Введите новый секретный ключ"
            imDialogIcon.setImageDrawable(resources.getDrawable(R.drawable.key_icon))

            edTextField.hint = "Секретный ключ"
            edConfirmSK.hint = "Подтвердите секретный ключ"
            edConfirmSK.visibility = View.VISIBLE
            btnCancel.visibility = View.GONE
            edTextField.transformationMethod = PasswordTransformationMethod.getInstance()
            edConfirmSK.transformationMethod = PasswordTransformationMethod.getInstance()

            btnAccept.text = "Подтвердить"
            btnAccept.setOnClickListener {
                if (edTextField.text.isNotEmpty() && edConfirmSK.text.isNotEmpty()) {
                    if (edConfirmSK.text.toString() == edTextField.text.toString()) {
                        prefs.edit().putString("secret_key", edTextField.text.toString()).apply()
                        result = true
                        dialog.dismiss()
                        return@setOnClickListener
                    } else {
                        tvErrorMsg.visibility = View.VISIBLE
                        tvErrorMsg.text = "Ключи не совпадают"
                    }
                } else {
                    tvErrorMsg.visibility = View.VISIBLE
                    tvErrorMsg.text = "Одно из полей пустое"
                }
            }
        }

        btnCancel.setOnClickListener {
            result = false
            dialog.dismiss()
            return@setOnClickListener
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        return result
    }
}


