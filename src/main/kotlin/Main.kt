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
        when (getAnswerNumber()) {
            0 -> break
            1 -> {
                println("Для выхода из режима  - 0")
                do {
                    var sizeListToScreen = NUMBER_UNLEARNED_WORDS_SCREEN
                    var unlearnedList =
                        dictionary
                            .shuffled()
                            .filter { it.correctAnswersCount < NUMBER_CORRECTLY_LEARNED }.toMutableList()

                    val sizeUnlearnedList = NUMBER_UNLEARNED_WORDS_SCREEN - unlearnedList.size
                    if (sizeUnlearnedList > 0) {
                        val learnedList = dictionary.filter { it.correctAnswersCount >= NUMBER_CORRECTLY_LEARNED }
                        unlearnedList += if (learnedList.size >= sizeUnlearnedList) {
                            learnedList.take(sizeUnlearnedList)
                        } else {
                            learnedList
                        }
                    }
                    if (unlearnedList.size < NUMBER_UNLEARNED_WORDS_SCREEN) sizeListToScreen = unlearnedList.size

                    unlearnedList = unlearnedList.shuffled().take(sizeListToScreen).toMutableList()

                    var indexWordRightAnswer: Int
                    do {
                        indexWordRightAnswer = (0 until sizeListToScreen).random()
                        if (unlearnedList[indexWordRightAnswer].correctAnswersCount < NUMBER_CORRECTLY_LEARNED)
                            break
                    } while (true)

                    println(unlearnedList[indexWordRightAnswer].original)
                    for (index in 0 until sizeListToScreen - 1) {
                        print("${index + 1} - ${unlearnedList[index].translated}, ")
                    }
                    println("$sizeListToScreen - ${unlearnedList[sizeListToScreen - 1].translated}")

                    val inputAnswer = getAnswerNumber()
                    if (inputAnswer == 0) break
                    if (indexWordRightAnswer == (inputAnswer - 1)) {
                        println("Правильно!")
                        dictionary[dictionary.indexOfFirst { it == unlearnedList[indexWordRightAnswer] }].correctAnswersCount++
                    } else {
                        println("Неправильно - слово [ ${unlearnedList[indexWordRightAnswer].translated} ]")
                    }
                } while (hasUnlearnedWords(dictionary))
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
const val NUMBER_UNLEARNED_WORDS_SCREEN = 4

fun hasUnlearnedWords(dictionary: MutableList<Word>): Boolean {
    return if (dictionary.count { it.correctAnswersCount < NUMBER_CORRECTLY_LEARNED } > 0) {
        true
    } else {
        println("Вы выучили все слова!")
        false
    }
}

fun getAnswerNumber(): Int {
    val input = readln()
    var inputInt = 99
    return try {
        inputInt = input.toInt()
        inputInt
    } catch (_: NumberFormatException) {
        inputInt
    }
}

