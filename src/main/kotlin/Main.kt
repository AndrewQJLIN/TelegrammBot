import java.io.File

fun main() {
    val dictionary: MutableList<Word> = mutableListOf()

    val wordsFile = File("words.txt")
    wordsFile.forEachLine {
        val line = it.split("|")
        dictionary += Word(original = line[0], translated = line[1], correctAnswersCount = line[2].toIntOrNull() ?: 0)
    }
    dictionary.forEach { println(it) }
}

data class Word(
    val original: String,
    val translated: String,
    val correctAnswersCount: Int = 0,
)

