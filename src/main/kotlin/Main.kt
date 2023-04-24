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
                    val unlearnedList =
                        dictionary
                            .shuffled()
                            .filter { it.correctAnswersCount < NUMBER_CORRECTLY_LEARNED }.toMutableList()
// переменная unlearnedList содержит список невыученных слов, т.е. тех которые удовлетворяют условию и если число таких слов
// будет меньше чем число вариантов которые надо вывести на экран, то в этот список надо добавить выученные слова  - так написано в задании
// на YOUTUBE это дальше и проверяется
// и к списку unlearnedList надо будет добавить новый список из выученных слов - вот поэтому он МУТАБЕЛЬНЫЙ
// переменная sizeUnlearnedList показывает сколько еще выученных слов надо добавить в список, чтобы он дозаполнился
                    val sizeUnlearnedList = NUMBER_UNLEARNED_WORDS_SCREEN - unlearnedList.size
                    if (sizeUnlearnedList > 0) {
                        val learnedList = dictionary.filter { it.correctAnswersCount >= NUMBER_CORRECTLY_LEARNED }
// если размер списка выученных слов такой что недостающая часть по количеству там есть - добавляем только недостающее число слов
                        unlearnedList += if (learnedList.size >= sizeUnlearnedList) {
                            learnedList.take(sizeUnlearnedList)
                        } else {
// а если этот список выученных слов маленький просто весь что есть
                            learnedList
                        }
                    }
                    if (unlearnedList.size < NUMBER_UNLEARNED_WORDS_SCREEN) sizeListToScreen = unlearnedList.size
//из полученного списка берем первые слова - там точно будут невыученные слова так как unlearnedList сначало заполнялся невыученными словами и потом мешаем его
// а то невыученное слово всегда будет первым
                    val listToScreen = unlearnedList.take(sizeListToScreen).shuffled()
// так как в итоговый список могут попасть уже выученные слова - то их не надо учить снова, их не надо показывать.
// а надо показывать только слова у которых
// число правильных ответов меньше заданного, вот в этом цикле они и ищутся.
// брать только первый - но тогда он и будет первым в списке.
                    var indexWordRightAnswer: Int
                    do {
                        indexWordRightAnswer = (0 until sizeListToScreen).random()
                        if (listToScreen[indexWordRightAnswer].correctAnswersCount < NUMBER_CORRECTLY_LEARNED)
                            break
                    } while (true)

                    println(listToScreen[indexWordRightAnswer].original)
                    for (index in 0 until sizeListToScreen - 1) {
                        print("${index + 1} - ${listToScreen[index].translated}, ")
                    }
                    println("$sizeListToScreen - ${listToScreen[sizeListToScreen - 1].translated}")

                    val inputAnswer = getAnswerNumber()
                    if (inputAnswer == 0) break
                    if (indexWordRightAnswer == (inputAnswer - 1)) {
                        println("Правильно!")
                        dictionary[dictionary.indexOfFirst { it == listToScreen[indexWordRightAnswer] }].correctAnswersCount++
                    } else {
                        println("Неправильно - слово [ ${listToScreen[indexWordRightAnswer].translated} ]")
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

fun getAnswerNumber(): Int = readln().toIntOrNull() ?: -1

