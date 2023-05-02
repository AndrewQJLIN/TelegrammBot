

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index, word -> "${index + 1} - ${word.translated}" }
        .joinToString(separator = "\n")
    return this.correctAnswer.original + "\n" + variants + "\n" + "0 - выйти в меню"
}

fun main() {
    val trainer = try {
        LearnWordsTrainer(numberOfWordsOfScreen = 4, numberCorrectlyLearned = 3)
    } catch (e: Exception) {
        println("Не возможно загрузить словарь")
        return
    }
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
                println("Выучено ${statistics.learnedWords} из ${statistics.totalWords} | ${statistics.percent}%")
            }
            else -> println("Введите только 1,2 или 0")
        }
    } while (true)
}






