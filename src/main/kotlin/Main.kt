import java.io.File

fun main() {
    val dictionary: MutableList<Word> = mutableListOf()
    val wordsFile = File("words.txt")
    wordsFile.forEachLine {
        val line = it.split("|")
        dictionary += Word(original = line[0], translated = line[1], correctAnswersCount = line[2].toIntOrNull() ?: 0)
    }
    do {
        println("Меню: 1 – Учить слова, 2 – Статистика, 0 – Выход")
        when (getAnswerNumber()) {
            0 -> break
            1 -> {
                println("Для выхода из режима  - 0")
                while (hasUnlearnedWords(dictionary)) {
                    val unlearnedWords =
                        dictionary
                            .shuffled()
                            .filter { it.correctAnswersCount < NUMBER_CORRECTLY_LEARNED }
                    val variants = unlearnedWords.take(NUMBER_OF_WORDS_ON_SCREEN).toMutableList()
                    val correctAnswer = variants.random()

                    if (variants.size < NUMBER_OF_WORDS_ON_SCREEN) {
                        variants += dictionary
                            .filter { it.correctAnswersCount >= NUMBER_CORRECTLY_LEARNED }
                            .shuffled()
                            .take(NUMBER_OF_WORDS_ON_SCREEN - variants.size)
                    }

                    println(correctAnswer.original)
                    variants.forEachIndexed { index, word ->
                        print("${index + 1} - ${word.translated}, ")
                    }
                    val inputAnswer = getAnswerNumber()
                    if (inputAnswer == 0) break
                    if (variants.indexOf(correctAnswer) == (inputAnswer - 1)) {
                        println("Правильно!")
                        correctAnswer.correctAnswersCount++
                    } else {
                        println("Неправильно - слово [ ${correctAnswer.translated} ]")
                    }
                }
            }

            2 -> {
                val learnedWords = dictionary.filter { it.correctAnswersCount >= NUMBER_CORRECTLY_LEARNED }.size
                val totalWords = dictionary.size
                println("Выучено $learnedWords из $totalWords | ${((learnedWords * 100 / totalWords))}%")
            }

            else -> println("Введите только 1,2 или 0")
        }
    } while (true)

    wordsFile.writeText("")
    dictionary.forEach {
        wordsFile.appendText("${it.original}|${it.translated}|${it.correctAnswersCount}\n")
    }
}

data class Word(
    val original: String,
    val translated: String,
    var correctAnswersCount: Int = 0,
)

const val NUMBER_CORRECTLY_LEARNED = 3
const val NUMBER_OF_WORDS_ON_SCREEN = 8

fun hasUnlearnedWords(dictionary: MutableList<Word>): Boolean {
    return if (dictionary.count { it.correctAnswersCount < NUMBER_CORRECTLY_LEARNED } > 0) {
        true
    } else {
        println("Вы выучили все слова!")
        false
    }
}

fun getAnswerNumber(): Int = readln().toIntOrNull() ?: -1

