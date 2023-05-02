import kotlinx.serialization.Serializable
import java.io.File
import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException

data class Statistics(
    val learnedWords: Int,
    val totalWords: Int,
    val percent: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word
)
@Serializable
data class Word(
    val original: String,
    val translated: String,
    var correctAnswersCount: Int = 0,
)
class LearnWordsTrainer(
    private val fileName:String = "words.txt",
    private val numberOfWordsOfScreen: Int,
    private val numberCorrectlyLearned: Int) {
    var question: Question? = null
    private val dictionary = loadDictionary()
    fun getStatistics(): Statistics {
        val learnedWords = dictionary.filter { it.correctAnswersCount >= numberCorrectlyLearned }.size
        val totalWords = dictionary.size
        val persent = learnedWords * 100 / totalWords
        return Statistics(learnedWords, totalWords, persent)
    }

    fun getNextQuestion(): Question? {
        val unlearnedWords =
            dictionary
                .shuffled()
                .filter { it.correctAnswersCount < numberCorrectlyLearned }
        if (unlearnedWords.isEmpty()) return null
        val variants = unlearnedWords.take(numberOfWordsOfScreen).toMutableList()
        val correctAnswer = variants.random()

        val finishList = if (variants.size < numberOfWordsOfScreen) {
            variants + dictionary
                .filter { it.correctAnswersCount >= numberCorrectlyLearned }
                .shuffled()
                .take(numberOfWordsOfScreen - variants.size)
        } else {
            variants
        }
            .shuffled()
        question = Question(
            variants = finishList,
            correctAnswer = correctAnswer
        )
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)
            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswersCount++
                saveDictionary()
                true
            } else {
                false
            }
        } ?: false
    }

    private fun loadDictionary(): List<Word> {
        try {
            val wordsFile = File(fileName)
            if(!wordsFile.exists()){
                File("words.txt").copyTo(wordsFile)
            }

            val dictionary = mutableListOf<Word>()
            wordsFile.forEachLine {
                val line = it.split("|")
                dictionary += Word(
                    original = line[0],
                    translated = line[1],
                    correctAnswersCount = line[2].toIntOrNull() ?: 0
                )
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("некорректный файл")
        }
    }

    private fun saveDictionary() {
        val wordsFile = File(fileName)
        wordsFile.writeText("")
        dictionary.forEach {
            wordsFile.appendText("${it.original}|${it.translated}|${it.correctAnswersCount}\n")
        }
    }

    fun resetProgress() {
        dictionary.forEach { it.correctAnswersCount=0 }
        saveDictionary()
    }
}




