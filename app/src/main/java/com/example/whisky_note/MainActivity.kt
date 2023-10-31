package com.example.whisky_note

import Note
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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

    fun addNewImageButton() {
        // DBHelper 인스턴스 생성
        val dbHelper = DBHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.query(DBHelper.TABLE_NAME, null, null, null, null, null, "${DBHelper.COLUMN_ID} DESC")

        buttonLayout.columnCount = 2

        if (cursor.moveToFirst()) {
            do {
                val note = Note(
                    id = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_ID)),
                    review = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_REVIEW)),
                    name = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NAME)),
                    nose = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_NOSE)),
                    palate = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_PALATE)),
                    finish = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_FINISH)),
                    rating = cursor.getFloat(cursor.getColumnIndex(DBHelper.COLUMN_RATING)),
                    imagePath = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_IMAGE_PATH)) ?: ""
                )

                val newImageButton = ImageButton(this).apply {
                    val buttonParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        width = 500
                        height = 500
                        leftMargin = 12
                        topMargin = 10
                    }
                    this.layoutParams = buttonParams
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    background=null
                    if (!note.imagePath.isNullOrEmpty()) {
                        Glide.with(this@MainActivity).load(note.imagePath).fitCenter().centerCrop().into(this)
                    }
                    setOnClickListener {
                        openReviewActivity(note)
                    }
                }

                val editTextNote = EditText(this).apply {
                    // EditText 설정...
                    setText(note.name ?: "")
                    val editTextParams = LinearLayout.LayoutParams(
                        500, // 가로 크기를 500으로 설정
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    this.layoutParams = editTextParams
                    gravity = Gravity.CENTER // 텍스트 가운데 정렬
                    isEnabled = false // 편집 불가능하도록 설정
                    setTextColor(Color.BLACK) // 글자 색상을 검정색으로 설정
                    background = null // 밑줄 제거
                }
                val starRating = RatingBar(this, null, android.R.attr.ratingBarStyleSmall).apply {
                    rating = note.rating ?: 0f // 노트 평점을 표시
                    numStars = 5 // 표시할 별 개수

                    val starSize = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        13f, // 별 하나의 크기를 15dp로 설정
                        resources.displayMetrics
                    )
                    val starPadding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        3f, // 별 이미지 사이에 추가할 여유분 크기를 3dp로 설정
                        resources.displayMetrics
                    )

                    val ratingBarParams = LinearLayout.LayoutParams(
                        ((starSize + starPadding) * numStars).toInt(), // 너비를 (별 크기 + 여유분)과 별 개수의 곱으로 설정
                        LinearLayout.LayoutParams.WRAP_CONTENT // 높이를 wrap_content로 설정
                    )
                    layoutParams = ratingBarParams

                    // 별 색상 변경
                    val color = Color.parseColor("#C51515") // 원하는 색상 값
                    progressTintList = ColorStateList.valueOf(color) // progressTint 속성에 색상 적용

                    setIsIndicator(true) // 사용자 입력을 받지 않고 단지 점수를 표시하는 용도로만 사용
                }

                val ratingBarContainer = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, // 너비를 match_parent로 설정
                        LinearLayout.LayoutParams.WRAP_CONTENT // 높이를 wrap_content로 설정
                    )
                    gravity = Gravity.CENTER // 중앙 정렬
                    addView(starRating) // RatingBar를 ratingBarContainer에 추가합니다.
                }
                val noteLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(newImageButton)
                    addView(editTextNote)
                    addView(ratingBarContainer) // ratingBarContainer를 추가합니다.
                }

                val params = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    leftMargin = 10
                    topMargin = 10
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)

                    // 한 행에 한 개씩 배치
                }

                noteLayout.layoutParams = params

                buttonLayout.addView(noteLayout)

            } while (cursor.moveToNext())
        }
        cursor.close()
    }




    fun main() {
        setContentView(R.layout.activity_main)

        val buttonCamera: ImageButton = findViewById(R.id.buttonCamera)
        buttonLayout = findViewById(R.id.buttonLayout)

        buttonLayout.columnCount = 2 // GridLayout의 columnCount를 2로 설정

        addNewImageButton() // addNewImageButton() 함수 호출

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
        val editTexts = listOf(editText_name, editText_nose, editText_palate, editText_finish, editText_review)

        editTexts.forEach { editText ->
            val hint = editText.hint.toString()
            val spannableString = SpannableString(hint)
            spannableString.setSpan(StyleSpan(Typeface.ITALIC), 0, hint.length, 0)
            editText.hint = spannableString
        }
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
            main()
        }
    }
}

