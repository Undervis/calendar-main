package com.example.addtocalendar

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.*

class EditDateActivity : AppCompatActivity() {
    private var imgPhoto: ImageView? = null
    private var bitmap: Bitmap? = null

    private var url: String = ""

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val stream: InputStream? = this.contentResolver.openInputStream(Uri.parse(it.toString()))
        val bufferStream = BufferedInputStream(stream)
        bitmap = BitmapFactory.decodeStream(bufferStream)

        imgPhoto!!.setImageBitmap(bitmap)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_date)

        val id = intent.getStringExtra("id")

        window.statusBarColor = resources.getColor(R.color.base_dark)

        imgPhoto = findViewById(R.id.img_photo)
        imgPhoto!!.clipToOutline = true

        val edTitle: EditText = findViewById(R.id.ed_title)
        val edDescription: EditText = findViewById(R.id.ed_desription)
        val btnAddDate: Button = findViewById(R.id.btn_add)
        val btnLoadImg: Button = findViewById(R.id.btn_load_img)

        val spMonth: Spinner = findViewById(R.id.spMonth)
        val edDay: EditText = findViewById(R.id.edDay)
        val edYear: EditText = findViewById(R.id.edYear)

        val btnBack: ImageButton = findViewById(R.id.btnReturnBack)
        btnBack.setOnClickListener { finish() }

        val months = MonthAdapter.getMonthArray()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, months)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        spMonth.adapter = adapter

        val prefs = getSharedPreferences("url", Context.MODE_PRIVATE)

        url = prefs.getString("server_url", "None").toString()

        val retrofitServices: RetrofitServices =
            RetrofitClient.getClient(url).create(RetrofitServices::class.java)

        val btnDeleteDate: TextView = findViewById(R.id.btn_delete)
        btnDeleteDate.setOnClickListener {
            val dialog = Dialog(this)

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setContentView(R.layout.dialog_layout)

            val btnAccept: TextView = dialog.findViewById(R.id.btnAccept)
            val btnCancel: TextView = dialog.findViewById(R.id.btnCancel)
            val tvGeneralMessage: TextView = dialog.findViewById(R.id.tvMessageDialog)
            val tvSecondMessage: TextView = dialog.findViewById(R.id.tvMessageScnd)
            val edTextField: EditText = dialog.findViewById(R.id.edTextField)
            val edConfirmSK: EditText = dialog.findViewById(R.id.edConfirmSK)
            val imDialogIcon: ImageView = dialog.findViewById(R.id.imDialogIcon)
            val tvErrorMsg: TextView = dialog.findViewById(R.id.tvErrorMsg)
            val pbConnection: ProgressBar = dialog.findViewById(R.id.pbConnection)

            btnAccept.text = "Удалить"
            tvGeneralMessage.text = "Удаление даты"
            tvSecondMessage.text = "Вы действительно хотите удалить дату?"
            edTextField.visibility = View.GONE
            edConfirmSK.visibility = View.GONE

            btnAccept.setOnClickListener {
                val deleteRequest = kotlinx.coroutines.MainScope()
                var deleteResult: Boolean
                deleteRequest.launch {
                    withContext(IO) { deleteResult = deleteDate(retrofitServices, id!!) }
                    if (deleteResult) {
                        dialog.dismiss()
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditDateActivity,
                            "Ошибка при удалении",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
        }

        var editedDate: DateClass
        val requestDate = kotlinx.coroutines.MainScope()
        requestDate.launch {
            withContext(IO) {
                editedDate = getDate(retrofitServices, id!!)
            }
            if (editedDate.id == -1) {
                Toast.makeText(
                    this@EditDateActivity,
                    "Не удалось загрузить дату",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                edTitle.setText(editedDate.title)
                edDescription.setText(editedDate.description)
                if (editedDate.day > 0) {
                    edDay.setText(editedDate.day.toString())
                }
                edYear.setText(editedDate.year.toString())
                spMonth.setSelection(editedDate.month)
                Picasso.get().load("$url/${editedDate.photo}").error(R.drawable.no_photo)
                    .into(imgPhoto)
            }
        }

        btnLoadImg.setOnClickListener {
            getImage.launch("image/*")
        }

        btnAddDate.setOnClickListener()
        {
            if (edYear.text.isEmpty() || edDay.text.isEmpty()
                || edDescription.text.isEmpty() || edTitle.text.isEmpty()
                || spMonth.selectedItemPosition == 0
            ) {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val day = edDay.text.toString().toInt()
            val month = spMonth.selectedItemPosition
            val year = edYear.text.toString().toInt()
            val title: String = edTitle.text.toString()
            val description: String = edDescription.text.toString()

            val date = DateClass(id!!.toInt(), day, month, year, title, description, "null")

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
                    var result: Boolean
                    var imgResult: Boolean
                    withContext(IO) {
                        result = saveDate(retrofitServices, date)
                    }
                    if (!result) {
                        Toast.makeText(
                            this@EditDateActivity,
                            "Не удалось сохранить дату",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        withContext(IO) {
                            imgResult = uploadImage(retrofitServices, Uri.parse(filePath), id)
                        }
                        if (imgResult) {
                            Toast.makeText(
                                this@EditDateActivity,
                                "Дата сохранена",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@EditDateActivity,
                                "Дата сохранена, но не удалось загрузить изображение",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } else {
                val request = kotlinx.coroutines.MainScope()
                request.launch {
                    var result: Boolean
                    withContext(Dispatchers.IO) {
                        result = saveDate(retrofitServices, date)
                    }
                    if (!result) {
                        Toast.makeText(
                            this@EditDateActivity,
                            "Не удалось сохранить дату",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@EditDateActivity,
                            "Дата сохранена",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        }
    }

    fun saveDate(services: RetrofitServices, date: DateClass): Boolean {
        val call: Call<DateClass> = services.editDate(date)
        return try {
            call.execute()
            true
        } catch (E: IOException) {
            false
        }

    }

    fun deleteDate(services: RetrofitServices, id: String): Boolean {
        val call: Call<ResponseBody> = services.deleteDate(id)
        return try {
            call.execute()
            true
        } catch (E: IOException) {
            false
        }
    }

    fun getDate(services: RetrofitServices, id: String): DateClass {
        val call: Call<DateClass> = services.getDate(id)
        return try {
            val date = call.execute().body()
            date!!
        } catch (E: IOException) {
            DateClass(-1, 0, 0, 0, "", "", "")
        }
    }

    fun uploadImage(services: RetrofitServices, fileUri: Uri, id: String): Boolean {
        val file = File(fileUri.toString())

        val requestFile: RequestBody =
            RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val filePart: MultipartBody.Part =
            MultipartBody.Part.createFormData("file", file.name, requestFile)
        val idPart: MultipartBody.Part =
            MultipartBody.Part.createFormData("id", id)
        val call: Call<ResponseBody> = services.uploadImage(filePart, idPart)
        return try {
            call.execute()
            true
        } catch (E: IOException) {
            false
        }

    }

}