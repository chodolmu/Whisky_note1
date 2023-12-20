package com.cj.whisky_note

import Note
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
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
    val CAMERA_AND_STORAGE_PERMISSION_V12 = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val CAMERA_AND_STORAGE_PERMISSION_V13 = arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    val FLAG_PERM_CAMERA = 98
    val YOUR_REQUEST_CODE = 1
    val FLAG_REQ_CAMERA = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main()
    }

    fun openReviewActivity(note: Note) {
        Intent(this@MainActivity, ReviewActivity::class.java).also { intent ->
            intent.putExtra("note", note)
            startActivityForResult(intent, YOUR_REQUEST_CODE)
        }
    }
    override fun onResume() {
        super.onResume()
        addNewImageButton()
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
                    "com.cj.whisky_note.FileProvider",
                    photoFile
                )

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, FLAG_REQ_CAMERA)
            }
        }
    }
    // MainActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                YOUR_REQUEST_CODE -> {
                    val removeNoteId = data?.getIntExtra("removeNoteId", -1)
                    if (removeNoteId != null && removeNoteId != -1) {
                        val imageButtonToRemove = findViewById<ImageButton>(removeNoteId)
                        if (imageButtonToRemove != null) {
                            (imageButtonToRemove.parent as ViewGroup).removeView(imageButtonToRemove)
                        } else {
                            main()
                        }
                    }
                }
                FLAG_REQ_CAMERA -> {
                    isCameraActivated = true
                    note(imagePath = currentPhotoPath)
                }
            }
        }
    }
    fun updateImageButton(updatedNote: Note) {
        val imageButtonTag = "ImageButton_" + updatedNote.id
        val buttonToUpdate = buttonLayout.findViewWithTag<ImageButton>(imageButtonTag)
        if (buttonToUpdate != null && !updatedNote.imagePath.isNullOrEmpty()) {
            Glide.with(this)
                .load(updatedNote.imagePath)
                .fitCenter()
                .centerCrop()
                .into(buttonToUpdate)
            buttonToUpdate.invalidate()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FLAG_PERM_CAMERA -> {
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
        buttonLayout.removeAllViews()
        val dbHelper = DBHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.query(DBHelper.TABLE_NAME, null, null, null, null, null, "${DBHelper.COLUMN_ID} DESC")
        buttonLayout.columnCount = 2

        if (cursor.moveToFirst()) {
            val columnIndexId = cursor.getColumnIndex(DBHelper.COLUMN_ID)
            val columnIndexReview = cursor.getColumnIndex(DBHelper.COLUMN_REVIEW)
            val columnIndexName = cursor.getColumnIndex(DBHelper.COLUMN_NAME)
            val columnIndexNose = cursor.getColumnIndex(DBHelper.COLUMN_NOSE)
            val columnIndexPalate = cursor.getColumnIndex(DBHelper.COLUMN_PALATE)
            val columnIndexFinish = cursor.getColumnIndex(DBHelper.COLUMN_FINISH)
            val columnIndexRating = cursor.getColumnIndex(DBHelper.COLUMN_RATING)
            val columnIndexImagePath = cursor.getColumnIndex(DBHelper.COLUMN_IMAGE_PATH)

            do {
                val id = if (columnIndexId != -1) cursor.getInt(columnIndexId) else 0
                val review = if (columnIndexReview != -1) cursor.getString(columnIndexReview) else ""
                val name = if (columnIndexName != -1) cursor.getString(columnIndexName) else ""
                val nose = if (columnIndexNose != -1) cursor.getString(columnIndexNose) else ""
                val palate = if (columnIndexPalate != -1) cursor.getString(columnIndexPalate) else ""
                val finish = if (columnIndexFinish != -1) cursor.getString(columnIndexFinish) else ""
                val rating = if (columnIndexRating != -1) cursor.getFloat(columnIndexRating) else 0f
                val imagePath = if (columnIndexImagePath != -1) cursor.getString(columnIndexImagePath) else ""

                val note = Note(
                    id = id,
                    review = review,
                    name = name,
                    nose = nose,
                    palate = palate,
                    finish = finish,
                    rating = rating,
                    imagePath = imagePath
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
                    setText(note.name ?: "")
                    val editTextParams = LinearLayout.LayoutParams(
                        500,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    this.layoutParams = editTextParams
                    gravity = Gravity.CENTER
                    isEnabled = false
                    setTextColor(Color.BLACK)
                    background = null
                    tag = note.id
                }
                val starRating = RatingBar(this, null, android.R.attr.ratingBarStyleSmall).apply {
                    numStars = 5
                    val starSize = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        13f,
                        resources.displayMetrics
                    )
                    val starPadding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        3f,
                        resources.displayMetrics
                    )

                    val ratingBarParams = LinearLayout.LayoutParams(
                        ((starSize + starPadding) * numStars).toInt(),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams = ratingBarParams

                    val color = Color.parseColor("#C51515")
                    progressTintList = ColorStateList.valueOf(color)

                    setIsIndicator(true)
                }
                starRating.rating = note.rating ?: 0f

                val ratingBarContainer = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    gravity = Gravity.CENTER
                    addView(starRating)
                }

                val noteLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    addView(newImageButton)
                    addView(editTextNote)
                    addView(ratingBarContainer)
                }

                val params = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    leftMargin = 10
                    topMargin = 10
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
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

        buttonLayout.columnCount = 2

        addNewImageButton() // addNewImageButton() 함수 호출

        buttonCamera.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (isPermitted(CAMERA_AND_STORAGE_PERMISSION_V13)) {
                    openCamera()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        CAMERA_AND_STORAGE_PERMISSION_V13,
                        FLAG_PERM_CAMERA
                    )
                }
            } else {
                if (isPermitted(CAMERA_AND_STORAGE_PERMISSION_V12)) {
                    openCamera()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        CAMERA_AND_STORAGE_PERMISSION_V12,
                        FLAG_PERM_CAMERA
                    )
                }
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
        val editTexts =
            listOf(editText_name, editText_nose, editText_palate, editText_finish, editText_review)

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
        starRating.rating = rating ?: 0f

        if (!imagePath.isNullOrEmpty()) {
            Glide.with(this).load(imagePath).centerCrop().into(imagePreview)
        }

        val btn_done = findViewById<Button>(R.id.button_done)
        btn_done.setOnClickListener {
            val reviewInput = editText_review.text.toString()
            val nameInput = editText_name.text.toString()
            val noseInput = editText_nose.text.toString()
            val palateInput = editText_palate.text.toString()
            val finishInput = editText_finish.text.toString()
            val ratingInput = starRating.rating

            val dbHelper = DBHelper(this@MainActivity)

            var values = ContentValues().apply {
                put(DBHelper.COLUMN_REVIEW, reviewInput)
                put(DBHelper.COLUMN_NAME, nameInput)
                put(DBHelper.COLUMN_NOSE, noseInput)
                put(DBHelper.COLUMN_PALATE, palateInput)
                put(DBHelper.COLUMN_FINISH, finishInput)
                put(DBHelper.COLUMN_RATING, ratingInput)

                if (currentPhotoPath != null) {
                    put(DBHelper.COLUMN_IMAGE_PATH, currentPhotoPath!!)
                }
            }
            val noteId =
                dbHelper.writableDatabase.insertOrThrow(DBHelper.TABLE_NAME, null, values).toInt()
            val updatedNote = Note(
                id = noteId,
                review = reviewInput ?: "",
                name = nameInput ?: "",
                nose = noseInput ?: "",
                palate = palateInput ?: "",
                finish = finishInput ?: "",
                rating = ratingInput,
                imagePath = currentPhotoPath ?: ""
            )

            if (isCameraActivated) {
                addNewImageButton()
                isCameraActivated = false
            } else {
                updateImageButton(updatedNote)
            }

            main()
        }
    }
}


