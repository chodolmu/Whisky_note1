package com.example.whisky_note

import Note
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
    fun openReviewActivity(note: Note) {
        Intent(this@MainActivity, ReviewActivity::class.java).also { intent ->
            intent.putExtra("note", note)
            startActivity(intent)
        }
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
                    note(imagePath = currentPhotoPath)
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

    fun addNewImageButton(){
        // DBHelper 인스턴스 생성
        val dbHelper = DBHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.query(DBHelper.TABLE_NAME, null, null, null, null,null,"${DBHelper.COLUMN_ID} DESC")

        if (cursor.moveToFirst()) {
            do {
                val newImageButton = ImageButton(this)
                val layoutParams = GridLayout.LayoutParams()
                layoutParams.width = 500
                layoutParams.height = 500
                layoutParams.rightMargin = 20
                layoutParams.leftMargin = 20
                layoutParams.topMargin = 40


                newImageButton.layoutParams = layoutParams


                // 여기서 각각의 imagePath를 사용하여 Glide로 로드합니다.
                val imagePathForButton: String? =
                    cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_IMAGE_PATH))
                if (!imagePathForButton.isNullOrEmpty()) {
                    Glide.with(this).load(imagePathForButton).centerCrop().into(newImageButton)
                }

                newImageButton.tag = Note(
                    id = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_ID)),  // Add this line
                    review = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_REVIEW)),
                    name = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NAME)),
                    nose = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NOSE)),
                    palate = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PALATE)),
                    finish=cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_FINISH)),
                    rating=cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_RATING)),
                    imagePath=imagePathForButton ?: ""
                )

                newImageButton.setOnClickListener{
                    openReviewActivity(it.tag as Note)
                }

                buttonLayout.addView(newImageButton)

            } while (cursor.moveToNext())
        }
        cursor.close()
    }
        fun main() {
            setContentView(R.layout.activity_main)

            val buttonCamera: ImageButton = findViewById(R.id.buttonCamera)
            buttonLayout = findViewById(R.id.buttonLayout)

            val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
            //val doneButtonCount = sharedPreferences.getInt("doneButtonCount", 0)

                addNewImageButton()

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
        imagePath: String? = null
    ) {
        setContentView(R.layout.activity_note)

        imagePreview = findViewById(R.id.imagePreview)
        editText_review = findViewById(R.id.editText_review)
        editText_name = findViewById(R.id.editText_name)
        editText_nose = findViewById(R.id.editText_nose)
        editText_palate = findViewById(R.id.editText_palate)
        editText_finish = findViewById(R.id.editText_finish)
        starRating = findViewById(R.id.ratingBar)

        editText_review.setText(review ?: "")
        editText_name.setText(name ?: "")
        editText_nose.setText(nose ?: "")
        editText_palate.setText(palate ?: "")
        editText_finish.setText(finish ?: "")
        starRating.rating=rating?:0f

        if (!imagePath.isNullOrEmpty()) {
            Glide.with(this).load(imagePath).centerCrop().into(imagePreview)
        }

        val btn_done=findViewById<Button>(R.id.button_done)

        btn_done.setOnClickListener{
            val reviewInput=editText_review.text.toString()
            val nameInput=editText_name.text.toString()
            val noseInput=editText_nose.text.toString()
            val palateInput=editText_palate.text.toString()
            val finishInput=editText_finish.text.toString()
            val ratingInput=starRating.rating

            val dbHelper=DBHelper(this@MainActivity)

            var values=ContentValues().apply{
                put(DBHelper.COLUMN_REVIEW,reviewInput )
                put(DBHelper.COLUMN_NAME,nameInput )
                put(DBHelper.COLUMN_NOSE,noseInput )
                put(DBHelper.COLUMN_PALATE,palateInput )
                put(DBHelper.COLUMN_FINISH,finishInput )
                put(DBHelper.COLUMN_RATING,ratingInput)

                if(currentPhotoPath !=null){
                    put (DBHelper.COLUMN_IMAGE_PATH,currentPhotoPath!!)
                }
            }

            dbHelper.writableDatabase.insertOrThrow(DBHelper.TABLE_NAME,null,values )

            if (isCameraActivated) {
                addNewImageButton()
                isCameraActivated=false
            }

            finish()
        }
    }
}

