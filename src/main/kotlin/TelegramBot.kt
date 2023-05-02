import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val LEARN_WORD_CLICKED = "learn_word_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val RESET_CLICKED = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)


fun main(args: Array<String>) {

    val json = Json { ignoreUnknownKeys = true }

    val botToken = args[0]
    val myBot = TelegramBotService(botToken)

    var lastUpdateId = 0L
    val trainers = HashMap<Long, LearnWordsTrainer>()

    val trainerBot = LearnWordsTrainer(numberCorrectlyLearned = 3, numberOfWordsOfScreen = 6)

    while (true) {
        val responseString = myBot.getUpdates(lastUpdateId)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, trainers, myBot) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdate(
    firstUpdate: Update,
    json: Json,
    trainers: HashMap<Long, LearnWordsTrainer>,
    myBot: TelegramBotService
) {
    val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id ?: return
    val text = firstUpdate.message?.text
    val data = firstUpdate.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) {
        LearnWordsTrainer("$chatId.txt", 6, 3)
    }

    if (text?.lowercase() == "/start") {
        myBot.sendMenu(json, chatId)
    }
    if (data?.lowercase() == STATISTICS_CLICKED) {
        val statistics = trainer.getStatistics()
        myBot.sendMessageToBot(
            json,
            chatId,
            "Выучено ${statistics.learnedWords} из ${statistics.totalWords} | ${statistics.percent}%"
        )
    }

    if (data?.lowercase() == LEARN_WORD_CLICKED) {
        myBot.checkNextQuestionAndSend(json, trainer, chatId)
    }

    if (data?.lowercase() == RESET_CLICKED) {
        trainer.resetProgress()
        myBot.sendMessageToBot(json, chatId, "Прогресс сброшен!")
    }


    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val inputAnswer = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(inputAnswer)) {
            myBot.sendMessageToBot(json, chatId, "Правильно!")
        } else {
            myBot.sendMessageToBot(
                json,
                chatId, "Неправильно! ${trainer.question?.correctAnswer?.original} " +
                        "- это [ ${trainer.question?.correctAnswer?.translated} ]"
            )
        }
        myBot.checkNextQuestionAndSend(json, trainer, chatId)
    }
}


class TelegramBotService(private val botToken: String) {
    companion object {
        const val PREFIX_URL = "https://api.telegram.org/bot"
    }

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$PREFIX_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()

    }

    fun sendMessageToBot(json: Json, chatId: Long, text: String): String {

        val encoded = URLEncoder.encode(
            text,
            StandardCharsets.UTF_8
        )
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = text,
        )
        val requestBodyString = json.encodeToString(requestBody)

        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMenu(json: Json, chatId: Long): String {
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "Изучать слова", callbackData = LEARN_WORD_CLICKED),
                        InlineKeyboard(text = "Статистика", callbackData = STATISTICS_CLICKED),
                    ),
                    listOf(
                        InlineKeyboard(text = "Сбросить статистику", callbackData = RESET_CLICKED),
                    )
                )
            )
        )

        val requestBodyString = json.encodeToString(requestBody)

        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendQuestion(json: Json, chatId: Long, question: Question): String {
        val urlSendMessage = "$PREFIX_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.original,
            replyMarkup = ReplyMarkup(
                listOf(question.variants.mapIndexed { index, word ->
                    InlineKeyboard(
                        text = word.translated,
                        callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"

                    )
                })
            )
        )

        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun checkNextQuestionAndSend(json: Json, trainerBot: LearnWordsTrainer, chatId: Long) {
        val question = trainerBot.getNextQuestion()
        if (question == null) {
            sendMessageToBot(json, chatId, "Вы выучили все слова!")
            return
        } else sendQuestion(json, chatId, question)
    }
}
