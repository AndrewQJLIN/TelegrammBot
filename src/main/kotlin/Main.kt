data class Word(
    val original: String,
    val translated: String,
    var correctAnswersCount: Int = 0,
)

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word -> "${index + 1} - ${word.translated}" }
        .joinToString(separator = "\n")
    return this.correctAnswer.original + "\n" + variants + "\n" + "0 - выйти в меню"
}

const val NUMBER_CORRECTLY_LEARNED = 3
const val NUMBER_OF_WORDS_ON_SCREEN = 4

fun main() {

    val trainer = LearnWordsTrainer()

    do {
        println("Меню: 1 – Учить слова, 2 – Статистика, 0 – Выход")
        when (readln().toIntOrNull()) {
            0 -> break
            1 -> {
                println("Для выхода из режима  - 0")
                while (true) {
                    val question = trainer.getNextQuestion()
                    if (question == null) {
                        println("Вы выучили все слова!")
                        break
                    } else {
                        println(question.asConsoleString())
                        val inputAnswer = readln().toIntOrNull()
                        if (inputAnswer == 0) break
                        if (trainer.checkAnswer(inputAnswer?.minus(1))) {
                            println("Правильно!")
                        } else {
                            println("Неправильно! ${question.correctAnswer.original} - это [ ${question.correctAnswer.translated} ]")
                        }
                    }
                }
            }
            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learnedWords} из ${statistics.totalWords} | ${statistics.persent}%")
            }
            else -> println("Введите только 1,2 или 0")
        }
    } while (true)
}






