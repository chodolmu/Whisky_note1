import java.io.Serializable

data class Note(
    val id: Int?,
    val review: String,
    val name: String,
    val nose: String,
    val palate: String,
    val finish: String,
    val rating: Float,
    val imagePath: String
) : Serializable