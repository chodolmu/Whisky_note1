package com.example.whisky_note

import Note
import android.content.ContentValues
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ReviewActivity : AppCompatActivity() {
    private lateinit var imagePreview: ImageView
    private lateinit var editText_review: EditText
    private lateinit var editText_name: EditText
    private lateinit var editText_nose: EditText
    private lateinit var editText_palate: EditText
    private lateinit var editText_finish: EditText
    private lateinit var starRating: RatingBar
    private lateinit var btn_remove: ImageButton

    // Note 객체 선언 (전역 변수로 사용)
    private lateinit var note : Note

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        imagePreview = findViewById(R.id.imagePreview)
        editText_review = findViewById(R.id.editText_review)
        editText_name = findViewById(R.id.editText_name)
        editText_nose = findViewById(R.id.editText_nose)
        editText_palate = findViewById(R.id.editText_palate)
        editText_finish = findViewById(R.id.editText_finish)
        starRating = findViewById(R.id.ratingBar)
        btn_remove = findViewById(R.id.button_remove)

        note = intent.getSerializableExtra("note") as Note

        displayNote(note)


        // Done button click listener 추가
        val btn_done=findViewById<Button>(R.id.button_done)

        btn_done.setOnClickListener {
            val reviewInput = editText_review.text.toString()
            val nameInput = editText_name.text.toString()
            val noseInput = editText_nose.text.toString()
            val palateInput = editText_palate.text.toString()
            val finishInput = editText_finish.text.toString()
            val ratingInput = starRating.rating

            // DBHelper 인스턴스 생성
            val dbHelper = DBHelper(this@ReviewActivity)

            // ContentValues에 데이터 담기
            var values = ContentValues().apply {
                put(DBHelper.COLUMN_REVIEW, reviewInput)
                put(DBHelper.COLUMN_NAME, nameInput)
                put(DBHelper.COLUMN_NOSE, noseInput)
                put(DBHelper.COLUMN_PALATE, palateInput)
                put(DBHelper.COLUMN_FINISH, finishInput)
                put(DBHelper.COLUMN_RATING, ratingInput)

                if (note.imagePath != null) {
                    put(DBHelper.COLUMN_IMAGE_PATH, note.imagePath!!)
                }
            }

            // Update the note in the database
            dbHelper.writableDatabase.update(
                DBHelper.TABLE_NAME,
                values,
                "${DBHelper.COLUMN_ID} = ?",
                arrayOf(note.id.toString())
            )

            finish() // 현재 액티비티 종료
        }
        btn_remove.setOnClickListener {
            val dbHelper = DBHelper(this@ReviewActivity)

            // 데이터베이스에서 해당 노트 삭제
            dbHelper.writableDatabase.delete(
                DBHelper.TABLE_NAME,
                "${DBHelper.COLUMN_ID} = ?",
                arrayOf(note.id.toString())
            )

            // 화면에서 동적으로 생성된 ImageButton 삭제
            note.id?.let { id ->
                val imageButtonToRemove = findViewById<ImageButton>(id)
                (imageButtonToRemove.parent as ViewGroup).removeView(imageButtonToRemove)
            }

            finish() // 현재 액티비티 종료
        }

        }


    fun displayNote(note: Note) {
        if (!note.imagePath.isNullOrEmpty()) {
            Glide.with(this).load(note.imagePath).centerCrop().into(imagePreview)
        }
        editText_review.setText(note.review ?: "")
        editText_name.setText(note.name ?: "")
        editText_nose.setText(note.nose ?: "")
        editText_palate.setText(note.palate ?: "")
        editText_finish.setText(note.finish ?: "")

        starRating.rating= note.rating ?: 0f
    }
}


