import java.io.File
import java.lang.NumberFormatException

fun main() {
    val dictionary: MutableList<Word> = mutableListOf()
    val wordsFile = File("words.txt")
    wordsFile.forEachLine {
        val line = it.split("|")
        dictionary += Word(original = line[0], translated = line[1], correctAnswersCount = line[2].toIntOrNull() ?: 0)
    }
    do {
        println("Меню: 1 – Учить слова, 2 – Статистика, 0 – Выход")
        val input = readln()
        var inputInt = 99
        try {
            inputInt = input.toInt()
        } catch (_: NumberFormatException) {
        }
        when (inputInt) {
            0 -> break
            1 -> println("Учим слова")
            2 -> {
                val learnedWords = dictionary.filter { it.correctAnswersCount >= 3 }.size
                val totalWords = dictionary.size
                println("Выучено $learnedWords из $totalWords | ${((learnedWords * 100 / totalWords))}%")
            }
            else -> println("Введите только 1,2 или 0")
        }
    } while (true)
}

data class Word(
    val original: String,
    val translated: String,
    val correctAnswersCount: Int = 0,
)

