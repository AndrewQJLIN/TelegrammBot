import java.io.File

data class Statistics(
    val learnedWords: Int,
    val totalWords: Int,
    val persent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word
)

class LearnWordsTrainer {
    private var question: Question? = null
    private val dictionary = loadDictionary()


    fun getStatistics(): Statistics {
        val learnedWords = dictionary.filter { it.correctAnswersCount >= NUMBER_CORRECTLY_LEARNED }.size
        val totalWords = dictionary.size
        val persent = learnedWords * 100 / totalWords
        return Statistics(learnedWords, totalWords, persent)

    }

    fun getNextQuestion(): Question? {
        val unlearnedWords =
            dictionary
                .shuffled()
                .filter { it.correctAnswersCount < NUMBER_CORRECTLY_LEARNED }
        if (unlearnedWords.isEmpty()) return null
        val variants = unlearnedWords.take(NUMBER_OF_WORDS_ON_SCREEN).toMutableList()
        val correctAnswer = variants.random()

        if (variants.size < NUMBER_OF_WORDS_ON_SCREEN) {
            variants += dictionary
                .filter { it.correctAnswersCount >= NUMBER_CORRECTLY_LEARNED }
                .shuffled()
                .take(NUMBER_OF_WORDS_ON_SCREEN - variants.size)
        }
        question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )
        return question

    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): List<Word> {
        val dictionary = mutableListOf<Word>()
        val wordsFile = File("words.txt")
        wordsFile.forEachLine {
            val line = it.split("|")
            dictionary += Word(
                original = line[0],
                translated = line[1],
                correctAnswersCount = line[2].toIntOrNull() ?: 0
            )
        }
        return dictionary
    }

    private fun saveDictionary(dictionary: List<Word>) {
        val wordsFile = File("words.txt")
        wordsFile.writeText("")
        dictionary.forEach {
            wordsFile.appendText("${it.original}|${it.translated}|${it.correctAnswersCount}\n")
        }
    }
}




