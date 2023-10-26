package com.example.whisky_note

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var imagePreview: ImageView
    private lateinit var editText_review: EditText
    private lateinit var editText_name: EditText
    private lateinit var editText_nose: EditText
    private lateinit var editText_palate: EditText
    private lateinit var editText_finish: EditText
    private lateinit var starRating: RatingBar
    private var currentPhotoPath: String? = null
    private lateinit var buttonLayout: GridLayout
    private var isCameraActivated = false

    val CAMERA_AND_STORAGE_PERMISSION = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val FLAG_PERM_CAMERA = 98
    val FLAG_PERM_STORAGE = 99

    val FLAG_REQ_CAMERA = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main()
    }

    fun isPermitted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun buttonClickEvent() {
        val dbHelper = DBHelper(this)
        val db = dbHelper.readableDatabase

        // 데이터베이스에서 데이터 조회
        val cursor = db.query(DBHelper.TABLE_NAME, null, null, null, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val review = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_REVIEW))
                val name = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NAME))
                val nose = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NOSE))
                val palate = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PALATE))
                val finish = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_FINISH))
                val rating = cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_RATING))
                val imagePath = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_IMAGE_PATH))

                setContentView(R.layout.activity_note)

                // 해당 뷰들을 초기화
                imagePreview = findViewById(R.id.imagePreview)
                editText_review = findViewById(R.id.editText_review)
                editText_name = findViewById(R.id.editText_name)
                editText_nose = findViewById(R.id.editText_nose)
                editText_palate = findViewById(R.id.editText_palate)
                editText_finish = findViewById(R.id.editText_finish)
                starRating = findViewById(R.id.ratingBar)

                editText_review.setText(review)
                editText_name.setText(name)
                editText_nose.setText(nose)
                editText_palate.setText(palate)
                editText_finish.setText(finish)
                starRating.rating = rating

                note(review, name, nose, palate, finish, rating, imagePath)

                if (!imagePath.isNullOrEmpty()) {
                    Glide.with(this).load(imagePath).into(imagePreview)
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
            }

            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.example.whisky_note.fileprovider",
                    photoFile
                )

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, FLAG_REQ_CAMERA)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FLAG_REQ_CAMERA -> {
                    note()
                    isCameraActivated = true // 카메라가 실행되었다는 것을 표시
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FLAG_PERM_CAMERA, FLAG_PERM_STORAGE -> {
                var checked = true
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        checked = false
                        break
                    }
                }
                if (checked) {
                    openCamera()
                }
            }
        }
    }

    fun addNewImageButton() {
        // 버튼을 동적으로 생성하고 레이아웃에 추가
        val newImageButton = ImageButton(this)
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 500 // ImageButton의 너비를 200px로 설정합니다.
        layoutParams.height = 500 // ImageButton의 높이를 200px로 설정합니다.
        layoutParams.rightMargin = 10
        layoutParams.leftMargin = 10
        layoutParams.bottomMargin = 10
        layoutParams.topMargin = 10
        newImageButton.layoutParams = layoutParams
        Glide.with(this).load(currentPhotoPath).into(newImageButton)
        newImageButton.setOnClickListener {
            buttonClickEvent()
        }
        buttonLayout.addView(newImageButton)
    }

        fun main() {
            setContentView(R.layout.activity_main)

            val buttonCamera: ImageButton = findViewById(R.id.buttonCamera)
            buttonLayout = findViewById(R.id.buttonLayout)

            val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
            val doneButtonCount = sharedPreferences.getInt("doneButtonCount", 0)

            for (i in 0 until doneButtonCount) {
                addNewImageButton()
            }

            buttonCamera.setOnClickListener {
                if (isPermitted(CAMERA_AND_STORAGE_PERMISSION)) {
                    openCamera()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        CAMERA_AND_STORAGE_PERMISSION,
                        FLAG_PERM_CAMERA
                    )
                }
            }
        }


    fun note(
        review: String? = null, name: String? = null,
        nose: String? = null, palate: String? = null,
        finish: String? = null, rating: Float? = null,
        imagePath: String? = null, bitmap: Bitmap? = null
    ) {
        setContentView(R.layout.activity_note)
        imagePreview = findViewById(R.id.imagePreview)
        editText_review = findViewById(R.id.editText_review)
        editText_name = findViewById(R.id.editText_name)
        editText_nose = findViewById(R.id.editText_nose)
        editText_palate = findViewById(R.id.editText_palate)
        editText_finish = findViewById(R.id.editText_finish)
        starRating = findViewById(R.id.ratingBar)



        // 값이 있으면 화면에 표시
        editText_review.setText(review ?: "")
        editText_name.setText(name ?: "")
        editText_nose.setText(nose ?: "")
        editText_palate.setText(palate ?: "")
        editText_finish.setText(finish ?: "")
        starRating.rating = rating ?: 0f
        if (!imagePath.isNullOrEmpty()) {
            Glide.with(this).load(imagePath).into(imagePreview)
        }

        val btn_done: Button = findViewById(R.id.button_done)
        val btn_remove: ImageButton = findViewById(R.id.button_remove)

        btn_done.setOnClickListener {
            val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            val review = editText_review.text.toString()
            val name = editText_name.text.toString()
            val nose = editText_nose.text.toString()
            val palate = editText_palate.text.toString()
            val finish = editText_finish.text.toString()
            val rating = starRating.rating

            // 데이터를 SharedPreferences에 저장
            editor.putString("editText_review", review)
            editor.putString("editText_name", name)
            editor.putString("editText_nose", nose)
            editor.putString("editText_palate", palate)
            editor.putString("editText_finish", finish)
            editor.putFloat("ratingBar", rating)
            editor.apply()

            // DBHelper 인스턴스 생성
            val dbHelper = DBHelper(this)
            val db = dbHelper.writableDatabase

            // ContentValues에 데이터 담기
            val values = ContentValues().apply {
                put(DBHelper.COLUMN_REVIEW, review)
                put(DBHelper.COLUMN_NAME, name)
                put(DBHelper.COLUMN_NOSE, nose)
                put(DBHelper.COLUMN_PALATE, palate)
                put(DBHelper.COLUMN_FINISH, finish)
                put(DBHelper.COLUMN_RATING, rating)
                put(DBHelper.COLUMN_IMAGE_PATH, currentPhotoPath)
            }

            // 데이터베이스에 데이터 삽입
            db.insert(DBHelper.TABLE_NAME, null, values)
            if (isCameraActivated) { // 카메라가 실행되었을 때만 버튼 추가
                val sharedPref = getSharedPreferences("MySharedPref", MODE_PRIVATE)
                val doneButtonCount = sharedPref.getInt("doneButtonCount", 0)
                sharedPref.edit().putInt("doneButtonCount", doneButtonCount + 1).apply()
                addNewImageButton()
                isCameraActivated = false // 버튼을 추가한 후에는 플래그를 다시 false로 설정
            }

            main()
        }
    }
}

