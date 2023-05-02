import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val LEARN_WORD_CLICKED = "learn_word_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
fun main(args: Array<String>) {

    val botToken = args[0]
    val myBot = TelegramBotService(botToken)

    val chatIdRegex = "\"chat\":\\{\"id\":(\\d+),".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()
    val updateIdRegex = "\"update_id\":(\\d+)".toRegex()
    var lastUpdateId = 0

    val trainerBot = LearnWordsTrainer(4, 3)

    while (true) {
        val updates = myBot.getUpdates(lastUpdateId)

        val checkUpdate = updateIdRegex.find(updates)?.groups?.get(1)?.value?.toIntOrNull() ?: continue
        lastUpdateId = checkUpdate + 1

        val chatId = chatIdRegex.find(updates)?.groups?.get(1)?.value?.toInt()
        val text = messageTextRegex.find(updates)?.groups?.get(1)?.value
        val data = dataRegex.find(updates)?.groups?.get(1)?.value

        if (text?.lowercase() == "/start" && chatId != null) {
            myBot.sendMenu(chatId)
        }
        if (data?.lowercase() == STATISTICS_CLICKED && chatId != null) {
            val statistics = trainerBot.getStatistics()
            myBot.sendMessageToBot(
                chatId,
                "Выучено ${statistics.learnedWords} из ${statistics.totalWords} | ${statistics.percent}%"
            )
        }

        if (data?.lowercase() == LEARN_WORD_CLICKED && chatId != null) {
            myBot.checkNextQuestionAndSend(trainerBot, chatId)
        }

        if (data != null) {
            if (data.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
                val inputAnswer = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
                if (trainerBot.checkAnswer(inputAnswer)) {
                    myBot.sendMessageToBot(chatId, "Правильно!")
                } else {
                    myBot.sendMessageToBot(
                        chatId, "Неправильно! ${trainerBot.question?.correctAnswer?.original} " +
                                "- это [ ${trainerBot.question?.correctAnswer?.translated} ]"
                    )
                }
                myBot.checkNextQuestionAndSend(trainerBot, chatId)
            }
        }
    }
}

class TelegramBotService(private val botToken: String) {
    companion object {
        const val PREFIX_URL = "https://api.telegram.org/bot"

    }

    fun getUpdates(updateId: Int): String {
        val urlGetUpdates = "$PREFIX_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()

    }

    fun sendMessageToBot(chatId: Int?, text: String?): String {
        val encoded = URLEncoder.encode(
            text,
            StandardCharsets.UTF_8
        )
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage?chat_id=$chatId&text=$encoded"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMenu(chatId: Int?): String {
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"
        val sendMenuBody = """
            {
                "chat_id": $chatId,
                "text": "Основное меню",
                "reply_markup": {
                    "inline_keyboard": [
                        [
                            {
                                "text": "Изучить слова",
                                "callback_data": "learn_word_clicked" 
                            },
                            {
                                "text": "Статистика",
                                "callback_data": "statistics_clicked"
                            }
                        ]
                    ]
                }
            }
        """.trimIndent()

        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendQuestion(chatId: Int?, question: Question): String {
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"
        val stringInner = "\n" + question.variants.mapIndexed { index, word ->
            "{\n" +
                    "\"text\": \"${word.translated}\",\n" +
                    "\"callback_data\": \"${CALLBACK_DATA_ANSWER_PREFIX + index.toString()}\"\n" +
                    "}"
        }
        val sendQuestionBody = """
            {
                "chat_id": $chatId,
                "text": "${question.correctAnswer.original}",
                "reply_markup": {
                    "inline_keyboard": [
                            $stringInner 
                        ]
                }
            }
        """.trimIndent()
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendQuestionBody))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun checkNextQuestionAndSend(trainerBot: LearnWordsTrainer, chatId: Int?) {
        val question = trainerBot.getNextQuestion()
        if (question == null) {
            sendMessageToBot(chatId, "Вы выучили все слова!")
            return
        } else sendQuestion(chatId, question)
    }
}
